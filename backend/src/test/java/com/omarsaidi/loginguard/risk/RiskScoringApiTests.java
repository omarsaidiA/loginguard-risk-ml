package com.omarsaidi.loginguard.risk;

import java.time.Instant;

import com.omarsaidi.loginguard.ApiTestSupport;
import com.omarsaidi.loginguard.user.MonitoredUser;
import com.omarsaidi.loginguard.user.MonitoredUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Features computed from real history in PostgreSQL, scored and explained through the API. */
@Transactional
class RiskScoringApiTests extends ApiTestSupport {

	private static final Instant MONDAY_NOON = Instant.parse("2026-10-05T12:00:00Z");

	@Autowired
	private MonitoredUserRepository users;

	private Long userId;

	@BeforeEach
	void createUser() {
		this.userId = this.users.save(new MonitoredUser("eve", "eve@example.com", "Sales")).getId();
	}

	private ResultActions login(Instant at, String country, String device, boolean successful) throws Exception {
		return this.mvc.perform(post("/api/login-events").with(analyst()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"userId": %d, "occurredAt": "%s", "ipAddress": "203.0.113.7", "countryCode": "%s",
					 "deviceId": "%s", "successful": %s}""".formatted(this.userId, at, country, device, successful)));
	}

	@Test
	void theFirstLoginOfAUserIsNotSuspiciousByItself() throws Exception {
		login(MONDAY_NOON, "FR", "laptop-1", true).andExpect(status().isCreated())
			.andExpect(jsonPath("$.risk.level").value("LOW"))
			.andExpect(jsonPath("$.risk.modelVersion").value("1.0.0"));
	}

	@Test
	void aUsualLoginIsLowRisk() throws Exception {
		login(MONDAY_NOON, "FR", "laptop-1", true);
		login(MONDAY_NOON.plusSeconds(20 * 3600), "FR", "laptop-1", true).andExpect(status().isCreated())
			.andExpect(jsonPath("$.risk.level").value("LOW"));
	}

	@Test
	void anAccountTakeoverIsFlaggedAndExplained() throws Exception {
		login(MONDAY_NOON, "FR", "laptop-1", true);
		// Password guessing from abroad, at night, 30 minutes after a normal login in France
		Instant night = Instant.parse("2026-10-06T02:00:00Z");
		login(MONDAY_NOON.plusSeconds(3600), "FR", "laptop-1", true);
		for (int i = 0; i < 5; i++) {
			login(night.minusSeconds(600 - i * 60L), "RU", "unknown-pc", false);
		}
		login(night, "BR", "unknown-pc-2", true).andExpect(status().isCreated())
			.andExpect(jsonPath("$.risk.level").value("HIGH"))
			.andExpect(jsonPath("$.risk.score", greaterThan(0.7)))
			.andExpect(jsonPath("$.risk.factors[0].value", greaterThan(0.0)))
			.andExpect(jsonPath("$.risk.factors[*].feature", hasItem("failed_attempts_1h")))
			.andExpect(jsonPath("$.risk.factors[*].feature", hasItem("rapid_country_change")));
	}

	@Test
	void eventsCanBeFilteredByRiskLevelAndCountedForTheDashboard() throws Exception {
		login(MONDAY_NOON, "FR", "laptop-1", true);
		this.mvc.perform(get("/api/login-events").param("level", "LOW").with(analyst()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].risk.level", hasItem("LOW")));
		this.mvc.perform(get("/api/stats").with(analyst()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total", greaterThan(0)))
			.andExpect(jsonPath("$.byRiskLevel.LOW", greaterThan(0)));
	}

	@Test
	void theModelCardIsAvailableToOperators() throws Exception {
		this.mvc.perform(get("/api/model").with(analyst()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.syntheticTrainingData").value(true))
			.andExpect(jsonPath("$.features[0].name").value("failed_attempts_1h"))
			.andExpect(jsonPath("$.metrics.rocAuc", greaterThan(0.5)));
		this.mvc.perform(get("/api/model")).andExpect(status().isUnauthorized());
	}

}
