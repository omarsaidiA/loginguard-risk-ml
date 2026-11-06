package com.omarsaidi.loginguard.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoredUserRepository extends JpaRepository<MonitoredUser, Long> {

	boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

}
