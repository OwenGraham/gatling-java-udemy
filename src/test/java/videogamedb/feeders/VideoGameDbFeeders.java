package videogamedb.feeders;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class VideoGameDbFeeders extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api")
            .acceptHeader("application/json");

//    private static FeederBuilder.FileBased<String> csvFeeder = csv("data/gameCsvFile.csv").circular(); // Read from the file data/gameCsvFile.csv and store the data in session variables

//    private static FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/gameJsonFile.json").circular();

    private static Iterator<Map<String, Object>> customFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                Random rand = new Random();
                int gameId = rand.nextInt(10 - 1 + 1) + 1;
                return Collections.singletonMap("gameId", gameId);
            }
            ).iterator();

    private static ChainBuilder getSpecificGame =
            feed(customFeeder) // Use the feeder to loop the call using the data from the file as session variables
                    .exec(http("Get video game with id - #{gameId}")
                    .get("/videogame/#{gameId}"));

    private static ScenarioBuilder scn = scenario("Video Game Db - Section 6 code")
            .repeat(10).on(
                    exec(getSpecificGame)
                            .pause(1)
            );

    {
        setUp(
                scn.injectOpen(atOnceUsers(1))
        ).protocols(httpProtocol);
    }
}
