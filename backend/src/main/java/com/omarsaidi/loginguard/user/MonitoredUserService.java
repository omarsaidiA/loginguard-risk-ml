package com.omarsaidi.loginguard.user;

import com.omarsaidi.loginguard.web.ConflictException;
import com.omarsaidi.loginguard.web.NotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MonitoredUserService {

	private final MonitoredUserRepository users;

	public MonitoredUserService(MonitoredUserRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public Page<MonitoredUser> list(Pageable pageable) {
		return this.users.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public MonitoredUser get(long id) {
		return this.users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
	}

	public MonitoredUser create(MonitoredUserDtos.Request request) {
		checkUnique(request, -1L);
		return this.users.save(new MonitoredUser(request.username(), normalize(request.email()), request.department()));
	}

	public MonitoredUser update(long id, MonitoredUserDtos.Request request) {
		MonitoredUser user = get(id);
		checkUnique(request, id);
		user.update(request.username(), normalize(request.email()), request.department());
		return user;
	}

	public void delete(long id) {
		this.users.delete(get(id));
	}

	private void checkUnique(MonitoredUserDtos.Request request, long id) {
		if (this.users.existsByUsernameIgnoreCaseAndIdNot(request.username(), id)) {
			throw new ConflictException("Username already exists");
		}
		if (this.users.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
			throw new ConflictException("Email already exists");
		}
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase(java.util.Locale.ROOT);
	}

}
