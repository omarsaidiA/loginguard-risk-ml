package com.omarsaidi.loginguard.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator from LOGINGUARD_ADMIN_PASSWORD, only while no operator exists.
 * There is no default password: without the variable, no account is created at all.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final OperatorRepository operators;

	private final PasswordEncoder passwordEncoder;

	private final String adminPassword;

	AdminBootstrap(OperatorRepository operators, PasswordEncoder passwordEncoder,
			@Value("${loginguard.admin.password:}") String adminPassword) {
		this.operators = operators;
		this.passwordEncoder = passwordEncoder;
		this.adminPassword = adminPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (this.operators.count() > 0) {
			return;
		}
		if (this.adminPassword.length() < 12 || this.adminPassword.length() > 72) {
			log.warn("No operator exists and LOGINGUARD_ADMIN_PASSWORD is missing or not 12 to 72 characters: "
					+ "nobody can sign in");
			return;
		}
		this.operators.save(new Operator("admin", this.passwordEncoder.encode(this.adminPassword), OperatorRole.ADMIN));
		log.info("Created the initial administrator account 'admin'");
	}

}
