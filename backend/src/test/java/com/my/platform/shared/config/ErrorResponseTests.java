package com.my.platform.shared.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// Runs against a real server: MockMvc does not perform the container's error dispatch to /error.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ErrorResponseTests {

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Test
	void csrfRejectionKeepsItsStatus() throws Exception {
		HttpResponse<String> response = postHealthWithoutCsrfToken();

		assertThat(response.statusCode()).isEqualTo(403);
	}

	@Test
	void errorBodyDoesNotExposeDetails() throws Exception {
		HttpResponse<String> response = postHealthWithoutCsrfToken();

		assertThat(response.body())
			.contains("\"status\":403", "\"error\":", "\"path\":")
			.doesNotContain("\"trace\"", "\"exception\"", "\"message\"");
	}

	private HttpResponse<String> postHealthWithoutCsrfToken() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
			.header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.noBody())
			.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
