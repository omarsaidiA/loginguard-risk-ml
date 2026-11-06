package com.omarsaidi.loginguard;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Full application against a real PostgreSQL (Testcontainers), with the real security filter
 * chain in front of MockMvc. Subclasses share one Spring context and one database container.
 */
@SpringBootTest(properties = { "loginguard.admin.password=Test-Admin-Password-1",
		"loginguard.security.login-attempts-per-minute=10000" })
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestSupport {

	@Autowired
	private WebApplicationContext context;

	protected MockMvc mvc;

	@BeforeEach
	void setUpMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).apply(springSecurity()).build();
	}

	protected static RequestPostProcessor admin() {
		return user("root").roles("ADMIN");
	}

	protected static RequestPostProcessor analyst() {
		return user("alice").roles("ANALYST");
	}

}
