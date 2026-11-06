package com.omarsaidi.loginguard.risk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

@Configuration
class RiskModelConfiguration {

	private static final Logger log = LoggerFactory.getLogger(RiskModelConfiguration.class);

	/**
	 * Loads the model only if its SHA-256 matches the pinned value. A model file is code that
	 * decides who gets flagged: a swapped or tampered file could silently blind the detection.
	 */
	@Bean
	RiskModel riskModel(JsonMapper jsonMapper, @Value("${loginguard.model.location}") Resource location,
			@Value("${loginguard.model.sha256}") String expectedSha256) throws IOException {
		byte[] content = location.getContentAsByteArray();
		String actual = sha256(content);
		byte[] expected = expectedSha256.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
		if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII), expected)) {
			throw new IllegalStateException(
					"Risk model integrity check failed: expected SHA-256 " + expectedSha256 + ", got " + actual);
		}
		RiskModel model = jsonMapper.readValue(content, RiskModel.class);
		model.validate();
		log.info("Loaded risk model {} ({}, sha256 {})", model.version(), model.algorithm(), actual);
		return model;
	}

	static String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

}
