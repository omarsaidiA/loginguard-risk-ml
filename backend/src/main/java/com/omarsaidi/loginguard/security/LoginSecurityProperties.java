package com.omarsaidi.loginguard.security;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Brute force protection settings, validated at start-up: a typo cannot silently disable them. */
@Validated
@ConfigurationProperties("loginguard.security")
public record LoginSecurityProperties(@Min(1) int maxFailedLogins, @NotNull Duration lockoutDuration,
		@Min(1) long loginAttemptsPerMinute) {
}
