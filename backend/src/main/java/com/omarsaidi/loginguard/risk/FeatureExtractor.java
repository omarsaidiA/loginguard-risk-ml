package com.omarsaidi.loginguard.risk;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.omarsaidi.loginguard.event.LoginEvent;
import com.omarsaidi.loginguard.event.LoginEventRepository;
import com.omarsaidi.loginguard.user.MonitoredUser;

import org.springframework.stereotype.Component;

/**
 * Computes the model's features from the history recorded BEFORE the login being scored, so an
 * event inserted late (or out of order) is judged on what was known at its own time.
 */
@Component
public class FeatureExtractor {

	private static final Duration FAILURE_WINDOW = Duration.ofHours(1);

	private static final Duration RAPID_COUNTRY_CHANGE = Duration.ofHours(2);

	private final LoginEventRepository events;

	public FeatureExtractor(LoginEventRepository events) {
		this.events = events;
	}

	public LoginFeatures extract(MonitoredUser user, Instant at, String countryCode, String deviceId,
			boolean successful) {
		Long userId = user.getId();
		int failures = (int) this.events.countByUserIdAndSuccessfulFalseAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
				userId, at.minus(FAILURE_WINDOW), at);
		Optional<LoginEvent> previous = this.events.findFirstByUserIdAndOccurredAtLessThanOrderByOccurredAtDesc(userId,
				at);

		// Without any history, nothing can be "new": the first login is not suspicious by itself
		boolean hasHistory = previous.isPresent();
		boolean newDevice = hasHistory
				&& !this.events.existsByUserIdAndDeviceIdAndOccurredAtLessThan(userId, deviceId, at);
		boolean newCountry = hasHistory
				&& !this.events.existsByUserIdAndCountryCodeAndOccurredAtLessThan(userId, countryCode, at);
		boolean rapidCountryChange = previous
			.filter(p -> !p.getCountryCode().equals(countryCode))
			.filter(p -> Duration.between(p.getOccurredAt(), at).compareTo(RAPID_COUNTRY_CHANGE) < 0)
			.isPresent();
		double hoursSinceLast = previous.map(p -> Duration.between(p.getOccurredAt(), at).toSeconds() / 3600.0)
			.orElse(0.0);
		int hour = at.atZone(ZoneOffset.UTC).getHour();

		return new LoginFeatures(failures, newDevice, newCountry, rapidCountryChange, hour < 6, hoursSinceLast,
				!successful);
	}

}
