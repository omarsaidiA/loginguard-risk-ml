package com.omarsaidi.loginguard.web;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem detail. Messages are written here, never copied from
 * exceptions, so nothing about the implementation (class names, SQL, stack traces) leaks.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}

	/** ?sort= on a property that does not exist: a client error, not a server error. */
	@ExceptionHandler(PropertyReferenceException.class)
	ProblemDetail unknownSortProperty() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Unknown sort property");
	}

	/** Race between the uniqueness check and the insert: the database constraint is the last line. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integrityViolation() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request conflicts with existing data");
	}

	// @PreAuthorize failures reach the controller advice first: answer them with the right status
	// instead of letting the catch-all below turn them into 500
	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail accessDenied() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail unauthenticated() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception exception) {
		log.error("Unhandled error", exception);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
	}

	/** Bean Validation errors: list the invalid fields with the messages defined on the DTOs. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
		List<Map<String, String>> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
			.toList();
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

}
