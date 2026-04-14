package smhy;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@IntegrationTest
@MockServerTest({
        "betterstack.heartbeat.url=http://localhost:${mockServerPort}/heartbeat",
        "smhy.forecasts.yr.url=http://localhost:${mockServerPort}/weatherapi/locationforecast/2.0/complete?altitude={altitude}&lat={latitude}&lon={longitude}",
        "smhy.forecasts.smhi.url=http://localhost:${mockServerPort}/api/category/pmp3g/version/2/geotype/point/lon/{longitude}/lat/{latitude}/data.json"
})
class ConsoleApplicationServiceTests {

	@Autowired
	private ConsoleApplicationService consoleApplicationService;

	private MockServerClient mockServerClient;

	@Test
	void testRun() {

		mockServerClient.when(
						request()
								.withMethod("GET")
								.withPath("/weatherapi/locationforecast/2.0/complete")
				)
				.respond(
						response()
								.withStatusCode(200)
								.withContentType(MediaType.APPLICATION_JSON)
								.withBody(readFromFile("yr/forecast.json")));

		mockServerClient.when(
						request()
								.withMethod("HEAD")
								.withPath("/heartbeat")
				)
				.respond(
						response()
								.withStatusCode(204));

		consoleApplicationService.run(new DefaultApplicationArguments());
	}

	private String readFromFile(String path) {
		try {
			return Files.readString(Paths.get("src/test/resources", path));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
