package com.omarsaidi.loginguard.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 401 and 403 raised by the security filters (before any controller) as problem details. The
 * bodies are constants: nothing from the request or the exception is echoed back.
 */
final class ProblemResponses {

	private ProblemResponses() {
	}

	static AuthenticationEntryPoint unauthorized() {
		return (request, response, exception) -> write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
	}

	static AccessDeniedHandler forbidden() {
		return (request, response, exception) -> write(response, HttpStatus.FORBIDDEN, "Access denied");
	}

	private static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter()
			.write("{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase() + "\",\"status\":"
					+ status.value() + ",\"detail\":\"" + detail + "\"}");
	}

}
