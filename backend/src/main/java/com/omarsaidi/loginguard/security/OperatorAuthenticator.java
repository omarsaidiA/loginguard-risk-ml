package com.omarsaidi.loginguard.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.omarsaidi.loginguard.operator.Operator;
import com.omarsaidi.loginguard.operator.OperatorRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks an operator's credentials and maintains the lockout counter.
 * <p>
 * Every failure gives the same answer: unknown user, wrong password, disabled or locked account
 * all raise the same exception, so the response does not reveal which accounts exist. A BCrypt
 * comparison runs in every case, so the response time does not reveal it either.
 */
@Component
public class OperatorAuthenticator {

	private final OperatorRepository operators;

	private final PasswordEncoder passwordEncoder;

	private final LoginSecurityProperties properties;

	private final Clock clock;

	private final String dummyHash;

	public OperatorAuthenticator(OperatorRepository operators, PasswordEncoder passwordEncoder,
			LoginSecurityProperties properties, Clock clock) {
		this.operators = operators;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("timing-equalization-only");
	}

	// The failure counter must be saved even though the method ends with an exception
	@Transactional(noRollbackFor = BadCredentialsException.class)
	public Authentication authenticate(String username, String password) {
		Instant now = Instant.now(this.clock);
		Optional<Operator> found = this.operators.findByUsername(username);
		if (found.isEmpty() || !found.get().isEnabled() || found.get().isLocked(now)) {
			this.passwordEncoder.matches(password, this.dummyHash);
			throw invalidCredentials();
		}
		Operator operator = found.get();
		if (!this.passwordEncoder.matches(password, operator.getPasswordHash())) {
			if (operator.registerFailedLogin(this.properties.maxFailedLogins(), this.properties.lockoutDuration(), now)) {
				SecurityEventLogger.accountLocked(username);
			}
			throw invalidCredentials();
		}
		operator.registerSuccessfulLogin();
		return UsernamePasswordAuthenticationToken.authenticated(operator.getUsername(), null,
				List.of(new SimpleGrantedAuthority("ROLE_" + operator.getRole().name())));
	}

	private static BadCredentialsException invalidCredentials() {
		return new BadCredentialsException("Invalid username or password");
	}

}
