package com.omarsaidi.loginguard.risk;

import java.util.List;

/**
 * The model's inputs for one login, computed from the user's history before that login.
 * {@link #NAMES} must match the features of the model file, in the same order.
 */
public record LoginFeatures(int failedAttemptsLastHour, boolean newDevice, boolean newCountry,
		boolean rapidCountryChange, boolean nightTime, double hoursSinceLastLogin, boolean attemptFailed) {

	public static final List<String> NAMES = List.of("failed_attempts_1h", "new_device", "new_country",
			"rapid_country_change", "night_time", "log_hours_since_last_login", "attempt_failed");

	public double[] toVector() {
		return new double[] { this.failedAttemptsLastHour, flag(this.newDevice), flag(this.newCountry),
				flag(this.rapidCountryChange), flag(this.nightTime),
				// Same transform as in ml/train.py: hours span several orders of magnitude
				Math.log1p(this.hoursSinceLastLogin), flag(this.attemptFailed) };
	}

	private static double flag(boolean value) {
		return value ? 1.0 : 0.0;
	}

}
