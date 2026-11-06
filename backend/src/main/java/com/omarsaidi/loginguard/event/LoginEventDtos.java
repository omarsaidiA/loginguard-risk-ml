package com.omarsaidi.loginguard.event;

import java.time.Instant;
import java.util.List;

import com.omarsaidi.loginguard.risk.RiskLevel;
import com.omarsaidi.loginguard.risk.RiskModel;
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

	/** The verdict and why: contributions in log-odds, highest first. */
	public record Risk(double score, RiskLevel level, String modelVersion, List<RiskModel.Contribution> factors) {
	}

	public record Response(Long id, Long userId, String username, Instant occurredAt, String ipAddress,
			String countryCode, String deviceId, boolean successful, Risk risk, ReviewStatus reviewStatus,
			String reviewedBy, Instant reviewedAt) {

		static Response from(LoginEvent event, RiskModel model) {
			Risk risk = null;
			if (event.getRiskScore() != null) {
				// Factors are recomputed from the stored features, only with the model that scored them
				List<RiskModel.Contribution> factors = model.version().equals(event.getModelVersion())
						? event.getFeatures().map(f -> model.assess(f).contributions()).orElse(List.of()) : List.of();
				risk = new Risk(event.getRiskScore(), event.getRiskLevel(), event.getModelVersion(), factors);
			}
			return new Response(event.getId(), event.getUser().getId(), event.getUser().getUsername(),
					event.getOccurredAt(), event.getIpAddress(), event.getCountryCode(), event.getDeviceId(),
					event.isSuccessful(), risk, event.getReviewStatus(), event.getReviewedBy(), event.getReviewedAt());
		}

	}

}
