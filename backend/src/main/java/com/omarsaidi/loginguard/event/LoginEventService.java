package com.omarsaidi.loginguard.event;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import com.omarsaidi.loginguard.risk.FeatureExtractor;
import com.omarsaidi.loginguard.risk.LoginFeatures;
import com.omarsaidi.loginguard.risk.RiskLevel;
import com.omarsaidi.loginguard.risk.RiskModel;
import com.omarsaidi.loginguard.security.SecurityEventLogger;
import com.omarsaidi.loginguard.user.MonitoredUser;
import com.omarsaidi.loginguard.user.MonitoredUserService;
import com.omarsaidi.loginguard.web.NotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginEventService {

	public record Stats(long total, Map<RiskLevel, Long> byRiskLevel, Map<ReviewStatus, Long> byReviewStatus,
			long pendingHighRisk) {
	}

	private final LoginEventRepository events;

	private final MonitoredUserService users;

	private final FeatureExtractor featureExtractor;

	private final RiskModel model;

	private final Clock clock;

	public LoginEventService(LoginEventRepository events, MonitoredUserService users, FeatureExtractor featureExtractor,
			RiskModel model, Clock clock) {
		this.events = events;
		this.users = users;
		this.featureExtractor = featureExtractor;
		this.model = model;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<LoginEvent> list(ReviewStatus status, RiskLevel level, Pageable pageable) {
		return this.events.search(status, level, pageable);
	}

	@Transactional(readOnly = true)
	public Page<LoginEvent> listForUser(long userId, Pageable pageable) {
		this.users.get(userId); // 404 for an unknown user rather than an empty page
		return this.events.findByUserId(userId, pageable);
	}

	@Transactional(readOnly = true)
	public LoginEvent get(long id) {
		return this.events.findById(id).orElseThrow(() -> new NotFoundException("Login event", id));
	}

	/** Records the event and scores it against the user's history before it. */
	public LoginEvent record(LoginEventDtos.Request request) {
		MonitoredUser user = this.users.get(request.userId());
		LoginFeatures features = this.featureExtractor.extract(user, request.occurredAt(), request.countryCode(),
				request.deviceId(), request.successful());
		LoginEvent event = new LoginEvent(user, request.occurredAt(), request.ipAddress(), request.countryCode(),
				request.deviceId(), request.successful());
		event.applyRisk(features, this.model.assess(features), this.model.version());
		return this.events.save(event);
	}

	public LoginEvent review(long id, ReviewStatus status, String reviewer) {
		LoginEvent event = get(id);
		event.review(status, reviewer, Instant.now(this.clock));
		SecurityEventLogger.eventReviewed(reviewer, id, status);
		return event;
	}

	public void delete(long id, String operator) {
		this.events.delete(get(id));
		SecurityEventLogger.eventDeleted(operator, id);
	}

	@Transactional(readOnly = true)
	public Stats stats() {
		Map<RiskLevel, Long> byLevel = new EnumMap<>(RiskLevel.class);
		Map<ReviewStatus, Long> byStatus = new EnumMap<>(ReviewStatus.class);
		for (RiskLevel level : RiskLevel.values()) {
			byLevel.put(level, 0L);
		}
		for (ReviewStatus status : ReviewStatus.values()) {
			byStatus.put(status, 0L);
		}
		long total = 0;
		long pendingHigh = 0;
		for (Object[] row : this.events.countByRiskLevelAndReviewStatus()) {
			RiskLevel level = (RiskLevel) row[0];
			ReviewStatus status = (ReviewStatus) row[1];
			long count = (Long) row[2];
			total += count;
			byStatus.merge(status, count, Long::sum);
			if (level != null) {
				byLevel.merge(level, count, Long::sum);
				if (level == RiskLevel.HIGH && status == ReviewStatus.PENDING) {
					pendingHigh += count;
				}
			}
		}
		return new Stats(total, byLevel, byStatus, pendingHigh);
	}

	public RiskModel model() {
		return this.model;
	}

}
