package com.omarsaidi.loginguard.event;

import java.time.Instant;

import com.omarsaidi.loginguard.web.IpAddress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public final class LoginEventDtos {

	private LoginEventDtos() {
	}

	public record Request(
			@NotNull @Positive Long userId,
			@NotNull @PastOrPresent Instant occurredAt,
			@NotBlank @IpAddress String ipAddress,
			@NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 code, e.g. FR")
			String countryCode,
			@NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$",
					message = "must be 1 to 64 letters, digits, '.', '_' or '-'") String deviceId,
			@NotNull Boolean successful) {
	}

	/** Only a final verdict can be set: an event cannot be put back to PENDING. */
	public record ReviewRequest(
			@NotNull @Pattern(regexp = "^(CONFIRMED_FRAUD|LEGITIMATE)$",
					message = "must be CONFIRMED_FRAUD or LEGITIMATE") String status) {
	}

	public record Response(Long id, Long userId, String username, Instant occurredAt, String ipAddress,
			String countryCode, String deviceId, boolean successful, ReviewStatus reviewStatus, String reviewedBy,
			Instant reviewedAt) {

		static Response from(LoginEvent event) {
			return new Response(event.getId(), event.getUser().getId(), event.getUser().getUsername(),
					event.getOccurredAt(), event.getIpAddress(), event.getCountryCode(), event.getDeviceId(),
					event.isSuccessful(), event.getReviewStatus(), event.getReviewedBy(), event.getReviewedAt());
		}

	}

}
