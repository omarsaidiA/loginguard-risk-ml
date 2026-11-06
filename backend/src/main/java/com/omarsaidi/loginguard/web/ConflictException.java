package com.omarsaidi.loginguard.web;

/** The request conflicts with existing data, such as a duplicate username (HTTP 409). */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
