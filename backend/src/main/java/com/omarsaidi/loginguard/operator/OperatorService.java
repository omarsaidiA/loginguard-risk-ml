package com.omarsaidi.loginguard.operator;

import java.util.List;

import com.omarsaidi.loginguard.security.SecurityEventLogger;
import com.omarsaidi.loginguard.web.ConflictException;
import com.omarsaidi.loginguard.web.NotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OperatorService {

	private final OperatorRepository operators;

	private final PasswordEncoder passwordEncoder;

	public OperatorService(OperatorRepository operators, PasswordEncoder passwordEncoder) {
		this.operators = operators;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<Operator> list() {
		return this.operators.findAll(Sort.by("username"));
	}

	public Operator create(OperatorDtos.CreateRequest request, String admin) {
		if (this.operators.existsByUsername(request.username())) {
			throw new ConflictException("Username already exists");
		}
		Operator operator = this.operators.save(new Operator(request.username(),
				this.passwordEncoder.encode(request.password()), request.role()));
		SecurityEventLogger.operatorCreated(admin, operator.getUsername(), operator.getRole());
		return operator;
	}

	public void delete(long id, String admin) {
		Operator operator = this.operators.findById(id).orElseThrow(() -> new NotFoundException("Operator", id));
		// An administrator cannot lock everyone out by deleting their own account
		if (operator.getUsername().equals(admin)) {
			throw new ConflictException("You cannot delete your own account");
		}
		this.operators.delete(operator);
		SecurityEventLogger.operatorDeleted(admin, operator.getUsername());
	}

}
