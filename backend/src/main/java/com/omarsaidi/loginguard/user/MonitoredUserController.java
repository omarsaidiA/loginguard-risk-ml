package com.omarsaidi.loginguard.user;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Analysts read monitored users; only administrators create, change or delete them. */
@RestController
@RequestMapping("/api/users")
public class MonitoredUserController {

	private final MonitoredUserService service;

	public MonitoredUserController(MonitoredUserService service) {
		this.service = service;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
	public PagedModel<MonitoredUserDtos.Response> list(
			@PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
		return new PagedModel<>(this.service.list(pageable).map(MonitoredUserDtos.Response::from));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
	public MonitoredUserDtos.Response get(@PathVariable long id) {
		return MonitoredUserDtos.Response.from(this.service.get(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public MonitoredUserDtos.Response create(@Valid @RequestBody MonitoredUserDtos.Request request) {
		return MonitoredUserDtos.Response.from(this.service.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public MonitoredUserDtos.Response update(@PathVariable long id,
			@Valid @RequestBody MonitoredUserDtos.Request request) {
		return MonitoredUserDtos.Response.from(this.service.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void delete(@PathVariable long id) {
		this.service.delete(id);
	}

}
