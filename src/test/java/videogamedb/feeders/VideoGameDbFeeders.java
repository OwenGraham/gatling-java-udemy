package videogamedb.feeders;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class VideoGameDbFeeders extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api")
            .acceptHeader("application/json");

    private static FeederBuilder.FileBased<String> csvFeeder = csv("data/gameCsvFile.csv").circular(); // Read from the file data/gameCsvFile.csv and store the data in session variables

    private static ChainBuilder getSpecificGame =
            feed(csvFeeder) // Use csvFeeder to loop the call using the data from the file as session variables
                    .exec(http("Get video game with name - #{gameName}")
                    .get("/videogame/#{gameId}")
                            .check(jsonPath("$.name").isEL("#{gameName}")));

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
