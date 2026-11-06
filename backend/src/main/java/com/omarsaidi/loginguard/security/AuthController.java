package com.omarsaidi.loginguard.security;

import java.util.OptionalLong;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session login for the single-page app. The session id lives in an HttpOnly cookie that
 * JavaScript cannot read (unlike a token in localStorage, an XSS cannot steal it), and every
 * state-changing request must carry the CSRF token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	public record LoginRequest(@NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 72) String password) {

		@Override
		public String toString() {
			return "LoginRequest[username=" + this.username + "]";
		}

	}

	public record Me(String username, String role) {

		static Me of(Authentication authentication) {
			String role = authentication.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
			return new Me(authentication.getName(), role);
		}

	}

	private final OperatorAuthenticator authenticator;

	private final LoginRateLimiter rateLimiter;

	private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

	private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

	public AuthController(OperatorAuthenticator authenticator, LoginRateLimiter rateLimiter) {
		this.authenticator = authenticator;
		this.rateLimiter = rateLimiter;
	}

	/** Lets the app obtain the CSRF cookie before its first POST (the login itself is protected). */
	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest login, HttpServletRequest request,
			HttpServletResponse response) {
		String ip = request.getRemoteAddr();
		OptionalLong retryAfter = this.rateLimiter.tryAcquire(ip);
		if (retryAfter.isPresent()) {
			SecurityEventLogger.loginRateLimited(ip);
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter.getAsLong()))
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, retry later"));
		}

		Authentication authentication;
		try {
			authentication = this.authenticator.authenticate(login.username(), login.password());
		}
		catch (BadCredentialsException exception) {
			SecurityEventLogger.loginFailed(login.username(), ip);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
		}

		// Session fixation: an id planted before the login must not become an authenticated session
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		SecurityContext context = this.contextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		this.contextHolder.setContext(context);
		this.contextRepository.saveContext(context, request, response);

		SecurityEventLogger.loginSucceeded(authentication.getName(), ip);
		return ResponseEntity.ok(Me.of(authentication));
	}

	@GetMapping("/me")
	public Me me(Authentication authentication) {
		return Me.of(authentication);
	}

}
