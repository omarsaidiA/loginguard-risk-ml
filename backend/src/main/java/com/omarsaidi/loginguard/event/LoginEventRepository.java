package com.omarsaidi.loginguard.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

	// The user is always displayed with its events: fetch it in the same query (no N+1)
	@EntityGraph(attributePaths = "user")
	Page<LoginEvent> findAllBy(Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Page<LoginEvent> findByReviewStatus(ReviewStatus status, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Page<LoginEvent> findByUserId(Long userId, Pageable pageable);

}
