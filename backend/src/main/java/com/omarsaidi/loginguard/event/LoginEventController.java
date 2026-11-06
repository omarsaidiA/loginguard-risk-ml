package com.omarsaidi.loginguard.event;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Analysts record and review login events; deleting one (destroying evidence) is admin only. */
@RestController
public class LoginEventController {

	private final LoginEventService service;

	public LoginEventController(LoginEventService service) {
		this.service = service;
	}

	@GetMapping("/api/login-events")
	@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
	public PagedModel<LoginEventDtos.Response> list(@RequestParam(required = false) ReviewStatus status,
			@PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(this.service.list(status, pageable).map(LoginEventDtos.Response::from));
	}

	@GetMapping("/api/users/{userId}/login-events")
	@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
	public PagedModel<LoginEventDtos.Response> listForUser(@PathVariable long userId,
			@PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(this.service.listForUser(userId, pageable).map(LoginEventDtos.Response::from));
	}

	@GetMapping("/api/login-events/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
	public LoginEventDtos.Response get(@PathVariable long id) {
		return LoginEventDtos.Response.from(this.service.get(id));
	}

	@PostMapping("/api/login-events")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
	public LoginEventDtos.Response record(@Valid @RequestBody LoginEventDtos.Request request) {
		return LoginEventDtos.Response.from(this.service.record(request));
	}

	@PutMapping("/api/login-events/{id}/review")
	@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
	public LoginEventDtos.Response review(@PathVariable long id,
			@Valid @RequestBody LoginEventDtos.ReviewRequest request, Principal principal) {
		// The reviewer is the authenticated operator, never a value sent by the client
		return LoginEventDtos.Response
			.from(this.service.review(id, ReviewStatus.valueOf(request.status()), principal.getName()));
	}

	@DeleteMapping("/api/login-events/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void delete(@PathVariable long id, Principal principal) {
		this.service.delete(id, principal.getName());
	}

}
