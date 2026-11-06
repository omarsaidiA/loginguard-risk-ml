package com.omarsaidi.loginguard.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IpAddressValidatorTests {

	private final IpAddressValidator validator = new IpAddressValidator();

	@ParameterizedTest
	@ValueSource(strings = { "203.0.113.7", "0.0.0.0", "255.255.255.255", "::1", "2001:db8::1",
			"::ffff:192.0.2.1", "fe80::1ff:fe23:4567:890a" })
	void acceptsLiteralAddresses(String value) {
		assertThat(this.validator.isValid(value, null)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "256.1.1.1", "1.2.3", "01.2.3.4", "localhost", "example.com", "1.2.3.4 ",
			"2001:db8::zz", ":::", "::1%eth0", "" })
	void rejectsAnythingElseWithoutResolvingIt(String value) {
		assertThat(this.validator.isValid(value, null)).isFalse();
	}

}
