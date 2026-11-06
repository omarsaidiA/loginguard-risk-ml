package com.omarsaidi.loginguard.operator;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

	Optional<Operator> findByUsername(String username);

	boolean existsByUsername(String username);

}
