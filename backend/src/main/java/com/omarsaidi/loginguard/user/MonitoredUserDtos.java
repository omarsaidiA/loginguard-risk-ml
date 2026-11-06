package com.omarsaidi.loginguard.user;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * API contract for monitored users. Requests and responses are separate records: a client can
 * never set the id or the creation date, and the entity is never serialized directly.
 */
public final class MonitoredUserDtos {

	private MonitoredUserDtos() {
	}

	public record Request(
			@NotBlank @Pattern(regexp = "^[a-z0-9._-]{3,50}$",
					message = "must be 3 to 50 lowercase letters, digits, '.', '_' or '-'") String username,
			@NotBlank @Email @Size(max = 254) String email,
			@Size(max = 50) @Pattern(regexp = "^[\\p{L}\\p{N} &'-]*$",
					message = "may only contain letters, digits, spaces, '&', ''' and '-'") String department) {
	}

	public record Response(Long id, String username, String email, String department, Instant createdAt) {

		static Response from(MonitoredUser user) {
			return new Response(user.getId(), user.getUsername(), user.getEmail(), user.getDepartment(),
					user.getCreatedAt());
		}

	}

}
