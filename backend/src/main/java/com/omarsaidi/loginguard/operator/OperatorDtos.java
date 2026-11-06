package com.omarsaidi.loginguard.operator;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OperatorDtos {

	private OperatorDtos() {
	}

	/** 12 to 72 characters: long passwords beat complexity rules, and BCrypt ignores bytes after 72. */
	public record CreateRequest(
			@NotBlank @Pattern(regexp = "^[a-z0-9._-]{3,50}$",
					message = "must be 3 to 50 lowercase letters, digits, '.', '_' or '-'") String username,
			@NotNull @Size(min = 12, max = 72) String password,
			@NotNull OperatorRole role) {

		/** Never print the password, even in a debug log of the request. */
		@Override
		public String toString() {
			return "CreateRequest[username=" + this.username + ", role=" + this.role + "]";
		}

	}

	public record Response(Long id, String username, OperatorRole role, boolean enabled, Instant lockedUntil,
			Instant createdAt) {

		static Response from(Operator operator) {
			return new Response(operator.getId(), operator.getUsername(), operator.getRole(), operator.isEnabled(),
					operator.getLockedUntil(), operator.getCreatedAt());
		}

	}

}
