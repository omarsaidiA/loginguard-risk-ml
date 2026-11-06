package com.omarsaidi.loginguard.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.omarsaidi.loginguard.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Over real HTTP: MockMvc's security support swaps the CSRF repository for a test one that never
 * writes the cookie, so the cookie's attributes can only be checked on a running server.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CsrfCookieTests {

	@LocalServerPort
	private int port;

	@Test
	void theCsrfCookieIsReadableByTheAppButNeverSentCrossSite() throws Exception {
		HttpResponse<Void> response = HttpClient.newHttpClient()
			.send(HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/api/auth/csrf")).build(),
					HttpResponse.BodyHandlers.discarding());

		String cookie = response.headers()
			.allValues("Set-Cookie")
			.stream()
			.filter(value -> value.startsWith("XSRF-TOKEN="))
			.findFirst()
			.orElseThrow();
		assertThat(cookie).contains("SameSite=Strict").contains("Secure").doesNotContain("HttpOnly");
	}

}
