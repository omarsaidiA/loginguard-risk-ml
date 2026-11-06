package com.omarsaidi.loginguard.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IpAddressValidator implements ConstraintValidator<IpAddress, String> {

	private static final Pattern IPV4 = Pattern
		.compile("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

	// Hexadecimal digits, colons and dots only: such a string is always parsed as a literal
	private static final Pattern IPV6_CHARACTERS = Pattern.compile("^[0-9A-Fa-f:.]{2,45}$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true; // @NotBlank reports missing values
		}
		if (IPV4.matcher(value).matches()) {
			return true;
		}
		if (!value.contains(":") || !IPV6_CHARACTERS.matcher(value).matches()) {
			return false;
		}
		// InetAddress.getByName would run a DNS lookup for a host name (a request the attacker
		// controls). The character check above guarantees it only parses a literal here.
		// (Java parses IPv4-mapped addresses such as ::ffff:192.0.2.1 into an Inet4Address: any
		// successful parse is a valid literal.)
		try {
			InetAddress.getByName(value);
			return true;
		}
		catch (UnknownHostException exception) {
			return false;
		}
	}

}
