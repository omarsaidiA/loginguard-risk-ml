package com.omarsaidi.loginguard.operator;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operator accounts, administrators only (also enforced at URL level in SecurityConfig). */
@RestController
@RequestMapping("/api/operators")
@PreAuthorize("hasRole('ADMIN')")
public class OperatorController {

	private final OperatorService service;

	public OperatorController(OperatorService service) {
		this.service = service;
	}

	@GetMapping
	public List<OperatorDtos.Response> list() {
		return this.service.list().stream().map(OperatorDtos.Response::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OperatorDtos.Response create(@Valid @RequestBody OperatorDtos.CreateRequest request, Principal principal) {
		return OperatorDtos.Response.from(this.service.create(request, principal.getName()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long id, Principal principal) {
		this.service.delete(id, principal.getName());
	}

}
