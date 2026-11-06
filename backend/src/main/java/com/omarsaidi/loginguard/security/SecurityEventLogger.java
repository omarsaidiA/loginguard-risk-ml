package com.omarsaidi.loginguard.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit trail on a dedicated "SECURITY" logger, so it can be routed to a SIEM on its own.
 * <p>
 * Only identifiers are logged: never a password, a session id or a CSRF token. Values that come
 * from the client are sanitised first: CR/LF would let an attacker forge extra log lines.
 */
public final class SecurityEventLogger {

	private static final Logger log = LoggerFactory.getLogger("SECURITY");

	private static final int MAX_LENGTH = 100;

	private SecurityEventLogger() {
	}

	public static void loginSucceeded(String username, String ip) {
		log.info("event=login_success user={} ip={}", sanitize(username), sanitize(ip));
	}

	public static void loginFailed(String username, String ip) {
		log.warn("event=login_failure user={} ip={}", sanitize(username), sanitize(ip));
	}

	public static void accountLocked(String username) {
		log.warn("event=account_locked user={}", sanitize(username));
	}

	public static void loginRateLimited(String ip) {
		log.warn("event=login_rate_limited ip={}", sanitize(ip));
	}

	public static void eventReviewed(String operator, long eventId, Object status) {
		log.info("event=login_event_reviewed operator={} id={} status={}", sanitize(operator), eventId, status);
	}

	public static void eventDeleted(String operator, long eventId) {
		log.info("event=login_event_deleted operator={} id={}", sanitize(operator), eventId);
	}

	public static void operatorCreated(String admin, String username, Object role) {
		log.info("event=operator_created by={} user={} role={}", sanitize(admin), sanitize(username), role);
	}

	public static void operatorDeleted(String admin, String username) {
		log.info("event=operator_deleted by={} user={}", sanitize(admin), sanitize(username));
	}

	static String sanitize(String value) {
		if (value == null) {
			return "-";
		}
		String cleaned = value.replaceAll("[\\r\\n\\t]", "_");
		return (cleaned.length() > MAX_LENGTH) ? cleaned.substring(0, MAX_LENGTH) + "..." : cleaned;
	}

}
