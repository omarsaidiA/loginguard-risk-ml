package com.omarsaidi.loginguard.event;

import java.time.Clock;
import java.time.Instant;

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

	private final LoginEventRepository events;

	private final MonitoredUserService users;

	private final Clock clock;

	public LoginEventService(LoginEventRepository events, MonitoredUserService users, Clock clock) {
		this.events = events;
		this.users = users;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<LoginEvent> list(ReviewStatus status, Pageable pageable) {
		return (status == null) ? this.events.findAllBy(pageable) : this.events.findByReviewStatus(status, pageable);
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

	public LoginEvent record(LoginEventDtos.Request request) {
		MonitoredUser user = this.users.get(request.userId());
		return this.events.save(new LoginEvent(user, request.occurredAt(), request.ipAddress(), request.countryCode(),
				request.deviceId(), request.successful()));
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

}
