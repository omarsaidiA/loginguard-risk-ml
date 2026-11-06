package com.omarsaidi.loginguard.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An account whose logins LoginGuard monitors. */
@Entity
@Table(name = "monitored_user")
public class MonitoredUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(length = 50)
	private String department;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected MonitoredUser() {
	}

	public MonitoredUser(String username, String email, String department) {
		this.username = username;
		this.email = email;
		this.department = department;
		this.createdAt = Instant.now();
	}

	public void update(String username, String email, String department) {
		this.username = username;
		this.email = email;
		this.department = department;
	}

	public Long getId() {
		return this.id;
	}

	public String getUsername() {
		return this.username;
	}

	public String getEmail() {
		return this.email;
	}

	public String getDepartment() {
		return this.department;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
