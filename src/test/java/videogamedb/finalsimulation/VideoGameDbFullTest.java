package videogamedb.finalsimulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class VideoGameDbFullTest extends Simulation {
    private static int gameId;

    // HTTP Protocol
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // Runtime Parameters
    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5")); // Store the system property USERS in a variable, and have it default to 5
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION","10")); // Store the system property RAMP_DURATION in a variable, and have it default to 10
    private static final int TEST_DURATION = Integer.parseInt(System.getProperty("TEST_DURATION","20")); // Store the system property TEST_DURATION in a variable, and have it default to 20

    // Feeders
    public static LocalDate randomDate(){
        int hundredYears = 100 * 365;
        return LocalDate.ofEpochDay(ThreadLocalRandom.current().nextInt(-hundredYears, hundredYears));
    }

    private static Iterator<Map<String, Object>> customFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        Random rand = new Random();
                        gameId = rand.nextInt(10 - 1 + 1) + 1;

                        String gameName = RandomStringUtils.randomAlphabetic(5) + "-gameName";
                        String releaseDate = randomDate().toString();
                        int reviewScore = rand.nextInt(100);
                        String category = RandomStringUtils.randomAlphabetic(5) + "-category";
                        String rating = RandomStringUtils.randomAlphabetic(4) + "-rating";

                        HashMap<String, Object> hmap = new HashMap<String, Object>();
                        hmap.put("gameId", gameId);
                        hmap.put("gameName", gameName);
                        hmap.put("releaseDate", releaseDate);
                        hmap.put("reviewScore", reviewScore);
                        hmap.put("category", category);
                        hmap.put("rating", rating);
                        return hmap;
                    }
            ).iterator();

    // Before Block
    @Override
    public void before(){
        System.out.printf("Running test with %d users%n", USER_COUNT);
        System.out.printf("Ramping users over %d seconds%n", RAMP_DURATION);
        System.out.printf("Total test duration: %d seconds", TEST_DURATION);
    }

    // HTTP calls
    private static ChainBuilder getAllVideoGames =
            exec(http("Get all video games")
                    .get("/videogame")
                    .check(status().is(200)));

    private static ChainBuilder authenticate =
            exec(http("Authenticate")
                    .post("/authenticate")
                    .body(StringBody("{\n" +
                            " \"password\": \"admin\",\n" +
                            " \"username\": \"admin\"\n" +
                            "}"))
                    .check(jsonPath("$.token").saveAs("jwtToken"))); // Save the token from the response as a variable called "jwtToken"

    private static ChainBuilder createNewGame =
            feed(customFeeder)
                    .exec(http("Create New Game - #{gameName}")
                            .post("/videogame")
                            .header("Authorization", "Bearer #{jwtToken}")
                            .body(ElFileBody("bodies/newGameTemplate.json")).asJson() // Convert the data from the feeder to JSON using a template
                            .check(bodyString().saveAs("responseBody")))
                    .exec(session -> {
                        System.out.println(session.getString("responseBody"));
                        return session;
                    });

    private static ChainBuilder getSpecificVideoGame =
            exec(http("Get specific video game")
                    .get("videogame/" + gameId));

    private static ChainBuilder deleteGame =
            exec(http("Delete video game")
                    .delete("videogame/" + gameId)
                    .check(bodyString().is("Video game deleted")));

    // Scenario or user journey
    // 1. Get all video games
    // 2. Create a new game
    // 3. Get details of newly created game
    // 4. delete newly created game

    private static ScenarioBuilder scn = scenario("Video Game Db - Section 8 code")
            .forever().on(
                    exec(getAllVideoGames)
                            .pause(5)
                            .exec(authenticate)
                            .pause(5)
                            .exec(createNewGame)
                            .pause(5)
                            .exec(getSpecificVideoGame)
                            .pause(5)
                            .exec(deleteGame)
            );

    // Load simulation
    {
        setUp(
                scn.injectOpen(
                        nothingFor(5), // Do nothing for 5 seconds
                        rampUsers(USER_COUNT).during(RAMP_DURATION)
                ).protocols(httpProtocol)
        ).maxDuration(TEST_DURATION);
    }

    // After block - print message
    @Override
    public void after(){
        System.out.println("Stress test finished");
    }
}
