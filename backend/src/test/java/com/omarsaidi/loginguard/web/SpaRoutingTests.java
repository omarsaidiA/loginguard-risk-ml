package com.omarsaidi.loginguard.web;

import com.omarsaidi.loginguard.ApiTestSupport;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaRoutingTests extends ApiTestSupport {

	@Test
	void deepLinksOfTheAppServeIndexHtml() throws Exception {
		for (String path : new String[] { "/login", "/events", "/events/12", "/users", "/model", "/operators" }) {
			this.mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
		}
	}

	@Test
	void otherUnknownPathsAreNotTurnedIntoTheApp() throws Exception {
		this.mvc.perform(get("/events/../../etc/passwd")).andExpect(status().isBadRequest());
		this.mvc.perform(get("/events/abc")).andExpect(status().isNotFound());
		this.mvc.perform(get("/wp-admin")).andExpect(status().isNotFound());
	}

}
