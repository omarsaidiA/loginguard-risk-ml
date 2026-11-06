package com.omarsaidi.loginguard.event;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.omarsaidi.loginguard.ApiTestSupport;
import com.omarsaidi.loginguard.user.MonitoredUser;
import com.omarsaidi.loginguard.user.MonitoredUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class LoginEventApiTests extends ApiTestSupport {

	@Autowired
	private MonitoredUserRepository users;

	@Autowired
	private LoginEventRepository events;

	private MonitoredUser user;

	@BeforeEach
	void createUser() {
		this.user = this.users.save(new MonitoredUser("jane", "jane@example.com", "IT"));
	}

	private String event(String ip, String country, Instant at) {
		return """
				{"userId": %d, "occurredAt": "%s", "ipAddress": "%s", "countryCode": "%s",
				 "deviceId": "laptop-42", "successful": true}""".formatted(this.user.getId(), at, ip, country);
	}

	@Test
	void analystRecordsAnEventAndReviewsIt() throws Exception {
		String id = this.mvc
			.perform(post("/api/login-events").with(analyst()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(event("203.0.113.7", "FR", Instant.now().minusSeconds(60))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value("jane"))
			.andExpect(jsonPath("$.reviewStatus").value("PENDING"))
			.andReturn()
			.getResponse()
			.getContentAsString()
			.replaceAll(".*\"id\":(\\d+).*", "$1");

		this.mvc
			.perform(put("/api/login-events/" + id + "/review").with(analyst()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				// A reviewer sent by the client is ignored: it is always the signed-in operator
				.content("""
						{"status": "CONFIRMED_FRAUD", "reviewedBy": "someone-else"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reviewStatus").value("CONFIRMED_FRAUD"))
			.andExpect(jsonPath("$.reviewedBy").value("alice"));

		this.mvc.perform(get("/api/login-events").param("status", "CONFIRMED_FRAUD").with(analyst()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", hasItem(Integer.parseInt(id))));
	}

	@Test
	void ipv6AddressesAreAccepted() throws Exception {
		this.mvc
			.perform(post("/api/login-events").with(analyst()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(event("2001:db8::1", "DE", Instant.now().minusSeconds(60))))
			.andExpect(status().isCreated());
	}

	@Test
	void invalidEventsAreRejected() throws Exception {
		Instant past = Instant.now().minusSeconds(60);
		String[][] cases = { { "999.1.1.1", "FR", past.toString() }, { "example.com", "FR", past.toString() },
				{ "203.0.113.7", "fr", past.toString() },
				{ "203.0.113.7", "FR", Instant.now().plus(1, ChronoUnit.DAYS).toString() } };
		for (String[] c : cases) {
			this.mvc
				.perform(post("/api/login-events").with(analyst()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content(event(c[0], c[1], Instant.parse(c[2]))))
				.andExpect(status().isBadRequest());
		}
	}

	@Test
	void eventForAnUnknownUserIsNotFound() throws Exception {
		this.mvc
			.perform(post("/api/login-events").with(analyst()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(event("203.0.113.7", "FR", Instant.now().minusSeconds(60))
					.replace("\"userId\": " + this.user.getId(), "\"userId\": 999999")))
			.andExpect(status().isNotFound());
	}

	@Test
	void anEventCannotBePutBackToPending() throws Exception {
		LoginEvent saved = this.events
			.save(new LoginEvent(this.user, Instant.now().minusSeconds(60), "203.0.113.7", "FR", "laptop-42", true));
		this.mvc
			.perform(put("/api/login-events/" + saved.getId() + "/review").with(analyst()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "PENDING"}"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void onlyAdministratorsCanDeleteEvidence() throws Exception {
		LoginEvent saved = this.events
			.save(new LoginEvent(this.user, Instant.now().minusSeconds(60), "203.0.113.7", "FR", "laptop-42", true));
		this.mvc.perform(delete("/api/login-events/" + saved.getId()).with(analyst()).with(csrf()))
			.andExpect(status().isForbidden());
		this.mvc.perform(delete("/api/login-events/" + saved.getId()).with(admin()).with(csrf()))
			.andExpect(status().isNoContent());
	}

	@Test
	void eventsOfAnUnknownUserAreNotFound() throws Exception {
		this.mvc.perform(get("/api/users/999999/login-events").with(analyst())).andExpect(status().isNotFound());
	}

}
