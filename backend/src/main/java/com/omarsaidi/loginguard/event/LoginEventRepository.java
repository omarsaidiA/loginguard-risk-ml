package com.omarsaidi.loginguard.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.omarsaidi.loginguard.risk.RiskLevel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

	// The user is always displayed with its events: fetch it in the same query (no N+1)
	@EntityGraph(attributePaths = "user")
	@Query("select e from LoginEvent e where (:status is null or e.reviewStatus = :status) "
			+ "and (:level is null or e.riskLevel = :level)")
	Page<LoginEvent> search(@Param("status") ReviewStatus status, @Param("level") RiskLevel level, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Page<LoginEvent> findByUserId(Long userId, Pageable pageable);

	// Feature queries: only the history strictly before the login being scored

	long countByUserIdAndSuccessfulFalseAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(Long userId, Instant from,
			Instant to);

	Optional<LoginEvent> findFirstByUserIdAndOccurredAtLessThanOrderByOccurredAtDesc(Long userId, Instant before);

	boolean existsByUserIdAndDeviceIdAndOccurredAtLessThan(Long userId, String deviceId, Instant before);

	boolean existsByUserIdAndCountryCodeAndOccurredAtLessThan(Long userId, String countryCode, Instant before);

	/** Rows of [riskLevel, reviewStatus, count] for the dashboard. */
	@Query("select e.riskLevel, e.reviewStatus, count(e) from LoginEvent e group by e.riskLevel, e.reviewStatus")
	List<Object[]> countByRiskLevelAndReviewStatus();

}
