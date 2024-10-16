package videogamedb.scriptfundamentals;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class VideoGameDb extends Simulation{

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private static ChainBuilder authenticate =
            exec(http("Authenticate")
                    .post("/authenticate")
                    .body(StringBody("{\n" +
                            " \"password\": \"admin\",\n" +
                            " \"username\": \"admin\"\n" +
                            "}"))
                    .check(jsonPath("$.token").saveAs("jwtToken"))); // Save the token from the response as a variable called "jwtToken"

    private static ChainBuilder createNewGame =
            exec(http("Create new game")
                    .post("/videogame")
                    .header("Authorization", "Bearer #{jwtToken}") // Add a header with the key "Authorisation" and the value "Bearer " + the value of the session variable jwtToken
                    .body(StringBody(
                            "{\n" +
                                    "  \"category\": \"Platform\",\n" +
                                    "  \"name\": \"Mario\",\n" +
                                    "  \"rating\": \"Mature\",\n" +
                                    "  \"releaseDate\": \"2012-05-04\",\n" +
                                    "  \"reviewScore\": 85\n" +
                                    "}"
                    )));

    private static ChainBuilder getAllVideoGames =
            exec(http("Get all video games")
                    .get("/videogame")
                    .check(status().not(404), status().not(500)));

    private static ChainBuilder getSpecificVideoGame =
            repeat(5,"myCounter").on( //Make this call 6 times, incrementing the value of myCounter each time
                    exec(http("Get specific video game")
                            .get("/videogame/#{myCounter}")
                            .check(status().in(200)))
            );

    private ScenarioBuilder scn = scenario("Video Game Db - Section5 code")
            .exec(getAllVideoGames)
            .pause(5)
            .exec(getSpecificVideoGame)
            .pause(5)
            .repeat(2).on(
                    exec(getAllVideoGames)
            )
            .exec(authenticate) // Get the authorization token
            .exec(createNewGame);

    {
        setUp(
                scn.injectOpen(atOnceUsers(1))
        ).protocols(httpProtocol);
    }
}
