package com.omarsaidi.loginguard.user;

import com.omarsaidi.loginguard.ApiTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class MonitoredUserApiTests extends ApiTestSupport {

	private static final String JOHN = """
			{"username": "john.doe", "email": "John.Doe@Example.com", "department": "Finance"}""";

	@Test
	void anonymousRequestsAreRejectedWithAProblemDetail() throws Exception {
		this.mvc.perform(get("/api/users"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Authentication required"));
	}

	@Test
	void adminCreatesReadsUpdatesAndDeletesAUser() throws Exception {
		String location = this.mvc
			.perform(post("/api/users").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JOHN))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value("john.doe"))
			.andExpect(jsonPath("$.email").value("john.doe@example.com"))
			.andReturn()
			.getResponse()
			.getContentAsString()
			.replaceAll(".*\"id\":(\\d+).*", "/api/users/$1");

		this.mvc.perform(get(location).with(analyst())).andExpect(status().isOk());
		this.mvc
			.perform(put(location).with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(JOHN.replace("Finance", "Audit")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.department").value("Audit"));
		this.mvc.perform(delete(location).with(admin()).with(csrf())).andExpect(status().isNoContent());
		this.mvc.perform(get(location).with(admin())).andExpect(status().isNotFound());
	}

	@Test
	void analystsCanReadButNotChangeUsers() throws Exception {
		this.mvc.perform(get("/api/users").with(analyst())).andExpect(status().isOk());
		this.mvc
			.perform(post("/api/users").with(analyst()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JOHN))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Access denied"));
	}

	@Test
	void invalidFieldsAreListedInTheError() throws Exception {
		this.mvc
			.perform(post("/api/users").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "John Doe", "email": "not-an-email", "department": "<script>"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field", hasItem("username")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("email")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("department")));
	}

	@Test
	void duplicateUsernameIsAConflict() throws Exception {
		this.mvc.perform(post("/api/users").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JOHN))
			.andExpect(status().isCreated());
		this.mvc
			.perform(post("/api/users").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(JOHN.replace("John.Doe@Example.com", "other@example.com")))
			.andExpect(status().isConflict());
	}

	@Test
	void writesWithoutTheCsrfTokenAreRejected() throws Exception {
		this.mvc.perform(post("/api/users").with(admin()).contentType(MediaType.APPLICATION_JSON).content(JOHN))
			.andExpect(status().isForbidden());
	}

	@Test
	void sortingOnAnUnknownPropertyIsAClientError() throws Exception {
		this.mvc.perform(get("/api/users").param("sort", "passwordHash").with(admin()))
			.andExpect(status().isBadRequest());
	}

}
