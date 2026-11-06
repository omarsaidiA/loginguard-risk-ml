package com.omarsaidi.loginguard.operator;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A person who signs in to LoginGuard. Only a BCrypt hash of the password is stored. */
@Entity
@Table(name = "operator")
public class Operator {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OperatorRole role;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "failed_logins", nullable = false)
	private int failedLogins;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Operator() {
	}

	public Operator(String username, String passwordHash, OperatorRole role) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = Instant.now();
	}

	public boolean isLocked(Instant now) {
		return this.lockedUntil != null && this.lockedUntil.isAfter(now);
	}

	/** Counts a failure; returns true when this failure locks the account. */
	public boolean registerFailedLogin(int maxFailures, Duration lockout, Instant now) {
		this.failedLogins++;
		if (this.failedLogins >= maxFailures) {
			this.failedLogins = 0;
			this.lockedUntil = now.plus(lockout);
			return true;
		}
		return false;
	}

	public void registerSuccessfulLogin() {
		this.failedLogins = 0;
		this.lockedUntil = null;
	}

	public Long getId() {
		return this.id;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public OperatorRole getRole() {
		return this.role;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public Instant getLockedUntil() {
		return this.lockedUntil;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
