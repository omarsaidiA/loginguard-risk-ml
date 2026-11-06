package com.omarsaidi.loginguard.security;

import com.omarsaidi.loginguard.ApiTestSupport;
import com.omarsaidi.loginguard.operator.Operator;
import com.omarsaidi.loginguard.operator.OperatorRepository;
import com.omarsaidi.loginguard.operator.OperatorRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AuthApiTests extends ApiTestSupport {

	private static final String PASSWORD = "Correct-Horse-Battery-9";

	@Autowired
	private OperatorRepository operators;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createOperator() {
		this.operators.save(new Operator("bob", this.passwordEncoder.encode(PASSWORD), OperatorRole.ANALYST));
	}

	private ResultActions login(String username, String password, MockHttpSession session) throws Exception {
		return this.mvc.perform(post("/api/auth/login").session(session).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password)));
	}

	@Test
	void loginOpensASessionAndRotatesItsId() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String idBeforeLogin = session.getId();

		login("bob", PASSWORD, session).andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("bob"))
			.andExpect(jsonPath("$.role").value("ANALYST"));

		// Session fixation: the id known before the login is no longer the authenticated one
		assertThat(session.getId()).isNotEqualTo(idBeforeLogin);
		this.mvc.perform(get("/api/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("bob"));
	}

	@Test
	void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
		String wrongPassword = login("bob", "wrong-password-123", new MockHttpSession())
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String unknownUser = login("nobody", "wrong-password-123", new MockHttpSession())
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(unknownUser).isEqualTo(wrongPassword);
	}

	@Test
	void accountIsLockedAfterFiveFailuresEvenForTheRightPassword() throws Exception {
		for (int attempt = 0; attempt < 5; attempt++) {
			login("bob", "wrong-password-123", new MockHttpSession()).andExpect(status().isUnauthorized());
		}
		login("bob", PASSWORD, new MockHttpSession()).andExpect(status().isUnauthorized());
		assertThat(this.operators.findByUsername("bob").orElseThrow().getLockedUntil()).isNotNull();
	}

	@Test
	void loginItselfRequiresTheCsrfToken() throws Exception {
		this.mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\": \"bob\", \"password\": \"%s\"}".formatted(PASSWORD)))
			.andExpect(status().isForbidden());
	}

	@Test
	void logoutEndsTheSession() throws Exception {
		MockHttpSession session = new MockHttpSession();
		login("bob", PASSWORD, session).andExpect(status().isOk());
		this.mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void failuresAreAuditedWithoutThePassword(CapturedOutput output) throws Exception {
		login("bob", "Sup3r-Secret-Guess", new MockHttpSession()).andExpect(status().isUnauthorized());
		assertThat(output).contains("event=login_failure user=bob").doesNotContain("Sup3r-Secret-Guess");
	}

	@Test
	void logInjectionIsNeutralised(CapturedOutput output) throws Exception {
		login("bob\\nevent=login_success user=admin", "wrong-password-123", new MockHttpSession());
		assertThat(output).doesNotContain("\nevent=login_success user=admin");
	}

	@Test
	void operatorManagementIsForAdministratorsOnly() throws Exception {
		this.mvc.perform(get("/api/operators").with(analyst())).andExpect(status().isForbidden());
		this.mvc
			.perform(post("/api/operators").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "carol", "password": "Long-Enough-Passw0rd", "role": "ANALYST"}"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void shortOperatorPasswordsAreRejected() throws Exception {
		this.mvc
			.perform(post("/api/operators").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "dave", "password": "short", "role": "ANALYST"}"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void theInitialAdministratorWasCreatedFromTheEnvironment() {
		assertThat(this.operators.findByUsername("admin")).hasValueSatisfying(
				operator -> assertThat(operator.getRole()).isEqualTo(OperatorRole.ADMIN));
	}

	@Test
	void securityHeadersAreSent() throws Exception {
		this.mvc.perform(get("/api/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
			.andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
			.andExpect(header().string("X-Frame-Options", "DENY"))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andExpect(header().string("Referrer-Policy", "no-referrer"));
	}

}
