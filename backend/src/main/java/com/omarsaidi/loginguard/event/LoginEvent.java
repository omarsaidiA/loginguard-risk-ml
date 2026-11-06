package com.omarsaidi.loginguard.event;

import java.time.Instant;
import java.util.Optional;

import com.omarsaidi.loginguard.risk.LoginFeatures;
import com.omarsaidi.loginguard.risk.RiskLevel;
import com.omarsaidi.loginguard.risk.RiskModel;
import com.omarsaidi.loginguard.user.MonitoredUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One login attempt of a monitored user. */
@Entity
@Table(name = "login_event")
public class LoginEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private MonitoredUser user;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "ip_address", nullable = false, length = 45)
	private String ipAddress;

	@Column(name = "country_code", nullable = false, length = 2)
	private String countryCode;

	@Column(name = "device_id", nullable = false, length = 64)
	private String deviceId;

	@Column(nullable = false)
	private boolean successful;

	@Enumerated(EnumType.STRING)
	@Column(name = "review_status", nullable = false, length = 20)
	private ReviewStatus reviewStatus = ReviewStatus.PENDING;

	@Column(name = "reviewed_by", length = 50)
	private String reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "failed_attempts_1h")
	private Integer failedAttemptsLastHour;

	@Column(name = "new_device")
	private Boolean newDevice;

	@Column(name = "new_country")
	private Boolean newCountry;

	@Column(name = "rapid_country_change")
	private Boolean rapidCountryChange;

	@Column(name = "night_time")
	private Boolean nightTime;

	@Column(name = "hours_since_last_login")
	private Double hoursSinceLastLogin;

	@Column(name = "risk_score")
	private Double riskScore;

	@Enumerated(EnumType.STRING)
	@Column(name = "risk_level", length = 10)
	private RiskLevel riskLevel;

	@Column(name = "model_version", length = 20)
	private String modelVersion;

	protected LoginEvent() {
	}

	public LoginEvent(MonitoredUser user, Instant occurredAt, String ipAddress, String countryCode, String deviceId,
			boolean successful) {
		this.user = user;
		this.occurredAt = occurredAt;
		this.ipAddress = ipAddress;
		this.countryCode = countryCode;
		this.deviceId = deviceId;
		this.successful = successful;
	}

	/** Stores the inputs with the verdict, so the score can be explained and audited later. */
	public void applyRisk(LoginFeatures features, RiskModel.Assessment assessment, String modelVersion) {
		this.failedAttemptsLastHour = features.failedAttemptsLastHour();
		this.newDevice = features.newDevice();
		this.newCountry = features.newCountry();
		this.rapidCountryChange = features.rapidCountryChange();
		this.nightTime = features.nightTime();
		this.hoursSinceLastLogin = features.hoursSinceLastLogin();
		this.riskScore = assessment.probability();
		this.riskLevel = assessment.level();
		this.modelVersion = modelVersion;
	}

	/** The stored features, or empty for an event recorded before scoring existed. */
	public Optional<LoginFeatures> getFeatures() {
		if (this.failedAttemptsLastHour == null) {
			return Optional.empty();
		}
		return Optional.of(new LoginFeatures(this.failedAttemptsLastHour, this.newDevice, this.newCountry,
				this.rapidCountryChange, this.nightTime, this.hoursSinceLastLogin, !this.successful));
	}

	public Double getRiskScore() {
		return this.riskScore;
	}

	public RiskLevel getRiskLevel() {
		return this.riskLevel;
	}

	public String getModelVersion() {
		return this.modelVersion;
	}

	/** Records who reviewed the event and when, for accountability. */
	public void review(ReviewStatus status, String reviewer, Instant at) {
		this.reviewStatus = status;
		this.reviewedBy = reviewer;
		this.reviewedAt = at;
	}

	public Long getId() {
		return this.id;
	}

	public MonitoredUser getUser() {
		return this.user;
	}

	public Instant getOccurredAt() {
		return this.occurredAt;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public String getCountryCode() {
		return this.countryCode;
	}

	public String getDeviceId() {
		return this.deviceId;
	}

	public boolean isSuccessful() {
		return this.successful;
	}

	public ReviewStatus getReviewStatus() {
		return this.reviewStatus;
	}

	public String getReviewedBy() {
		return this.reviewedBy;
	}

	public Instant getReviewedAt() {
		return this.reviewedAt;
	}

}
