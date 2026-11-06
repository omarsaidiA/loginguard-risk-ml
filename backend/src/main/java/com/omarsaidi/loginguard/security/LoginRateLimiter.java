package com.omarsaidi.loginguard.security;

import java.time.Duration;
import java.util.OptionalLong;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import org.springframework.stereotype.Component;

/**
 * Token bucket per client IP on the login endpoint. Account lockout stops guessing one account's
 * password; this stops one client from trying many accounts (password spraying).
 * <p>
 * The IP is the socket address, not X-Forwarded-For, which any client can forge. Buckets live in
 * a size-bounded cache so an attacker rotating addresses cannot exhaust memory.
 */
@Component
public class LoginRateLimiter {

	private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
		.maximumSize(100_000)
		.expireAfterAccess(Duration.ofMinutes(10))
		.build();

	private final long attemptsPerMinute;

	public LoginRateLimiter(LoginSecurityProperties properties) {
		this.attemptsPerMinute = properties.loginAttemptsPerMinute();
	}

	/** Empty when the attempt is allowed, otherwise the number of seconds to wait. */
	public OptionalLong tryAcquire(String ip) {
		Bucket bucket = this.buckets.get(ip, key -> Bucket.builder()
			.addLimit(Bandwidth.builder()
				.capacity(this.attemptsPerMinute)
				.refillGreedy(this.attemptsPerMinute, Duration.ofMinutes(1))
				.build())
			.build());
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds()));
	}

}
