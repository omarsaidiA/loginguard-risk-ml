package com.omarsaidi.loginguard.security;

import com.omarsaidi.loginguard.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Own context with a limit of 3 attempts per minute, so the other tests are not throttled. */
@SpringBootTest(properties = "loginguard.security.login-attempts-per-minute=3")
@Import(TestcontainersConfiguration.class)
class LoginRateLimitTests {

	@Autowired
	private WebApplicationContext context;

	private MockHttpServletRequestBuilder attemptFrom(String ip) {
		return post("/api/auth/login").with(csrf()).with(request -> {
			request.setRemoteAddr(ip);
			return request;
		}).contentType(MediaType.APPLICATION_JSON).content("""
				{"username": "anyone", "password": "guess-number-1"}""");
	}

	@Test
	void aClientIsThrottledAcrossAccountsButOthersAreNot() throws Exception {
		MockMvc mvc = MockMvcBuilders.webAppContextSetup(this.context).apply(springSecurity()).build();
		for (int attempt = 0; attempt < 3; attempt++) {
			mvc.perform(attemptFrom("198.51.100.1")).andExpect(status().isUnauthorized());
		}
		mvc.perform(attemptFrom("198.51.100.1"))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"));
		mvc.perform(attemptFrom("198.51.100.2")).andExpect(status().isUnauthorized());
	}

}
