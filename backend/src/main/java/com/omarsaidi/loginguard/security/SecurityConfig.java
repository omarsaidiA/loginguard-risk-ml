package com.omarsaidi.loginguard.security;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(LoginSecurityProperties.class)
public class SecurityConfig {

	// The app is built into static files served from this origin: no inline script or style, no
	// third-party origin, no framing
	private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; "
			+ "style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; "
			+ "base-uri 'none'; form-action 'self'; frame-ancestors 'none'";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Value("${server.servlet.session.cookie.secure}") boolean secureCookies) throws Exception {
		// The CSRF cookie must be readable by the app (no HttpOnly: that is the double-submit
		// pattern), but gets the same SameSite and Secure attributes as the session cookie.
		// Gap found by running headercheck against this application.
		CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokens.setCookieCustomizer(cookie -> cookie.sameSite("Strict").secure(secureCookies));

		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
			.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
			.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
			// Defense in depth: also checked by @PreAuthorize on the controller
			.requestMatchers("/api/operators/**").hasRole("ADMIN")
			.requestMatchers("/api/**", "/actuator/**").authenticated()
			// Everything else is the static single-page app, which holds no data
			.anyRequest()
			.permitAll())
			// Cookie-based CSRF token readable by the app and sent back in the X-XSRF-TOKEN header
			.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokens))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(logout -> logout.logoutUrl("/api/auth/logout")
				.deleteCookies("LG_SESSION")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.exceptionHandling(errors -> errors.authenticationEntryPoint(ProblemResponses.unauthorized())
				.accessDeniedHandler(ProblemResponses.forbidden()))
			.headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
				.frameOptions(frame -> frame.deny())
				.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
				.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
				.permissionsPolicyHeader(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()")));
		return http.build();
	}

	/** Cost 12: about 250 ms per hash, slow enough to make offline cracking expensive. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
