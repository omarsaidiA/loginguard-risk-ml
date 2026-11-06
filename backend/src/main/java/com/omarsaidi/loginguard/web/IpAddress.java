package com.omarsaidi.loginguard.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The value must be a literal IPv4 or IPv6 address (host names are rejected). */
@Documented
@Constraint(validatedBy = IpAddressValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface IpAddress {

	String message() default "must be a valid IPv4 or IPv6 address";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
