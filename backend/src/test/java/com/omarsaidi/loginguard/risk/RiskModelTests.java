package com.omarsaidi.loginguard.risk;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

/** Behaviour of the real model file shipped with the application. */
class RiskModelTests {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final ClassPathResource MODEL_FILE = new ClassPathResource("model/login-risk-model.json");

	private static RiskModel model;

	@BeforeAll
	static void loadShippedModel() throws Exception {
		String sha256 = RiskModelConfiguration.sha256(MODEL_FILE.getContentAsByteArray());
		model = new RiskModelConfiguration().riskModel(JSON, MODEL_FILE, sha256);
	}

	private static final LoginFeatures USUAL = new LoginFeatures(0, false, false, false, false, 20, false);

	private static final LoginFeatures TAKEOVER = new LoginFeatures(6, true, true, true, true, 24 * 60, true);

	@Test
	void aUsualLoginIsLowRisk() {
		assertThat(model.assess(USUAL).level()).isEqualTo(RiskLevel.LOW);
	}

	@Test
	void anAccountTakeoverPatternIsHighRisk() {
		RiskModel.Assessment assessment = model.assess(TAKEOVER);
		assertThat(assessment.level()).isEqualTo(RiskLevel.HIGH);
		assertThat(assessment.probability()).isGreaterThan(0.9);
	}

	@Test
	void eachWarningSignRaisesTheScore() {
		double base = model.assess(USUAL).probability();
		assertThat(model.assess(new LoginFeatures(0, true, false, false, false, 20, false)).probability())
			.isGreaterThan(base);
		assertThat(model.assess(new LoginFeatures(3, false, false, false, false, 20, false)).probability())
			.isGreaterThan(base);
		assertThat(model.assess(new LoginFeatures(0, false, false, true, false, 20, false)).probability())
			.isGreaterThan(base);
	}

	@Test
	void theExplanationAddsUpToTheScore() {
		RiskModel.Assessment assessment = model.assess(TAKEOVER);
		double logit = model.intercept()
				+ assessment.contributions().stream().mapToDouble(RiskModel.Contribution::value).sum();
		assertThat(1 / (1 + Math.exp(-logit))).isCloseTo(assessment.probability(), within(1e-12));
		// Factors are sorted, strongest push towards "risky" first
		assertThat(assessment.contributions().get(0).value())
			.isGreaterThanOrEqualTo(assessment.contributions().get(1).value());
	}

	@Test
	void aTamperedModelFileIsRefused() throws Exception {
		String genuineSha256 = RiskModelConfiguration.sha256(MODEL_FILE.getContentAsByteArray());
		// Same structure, one coefficient changed: e.g. to stop flagging new devices
		String tampered = MODEL_FILE.getContentAsString(StandardCharsets.UTF_8)
			.replaceFirst("\"coefficient\": [0-9.]+", "\"coefficient\": 0.0");
		assertThatIllegalStateException()
			.isThrownBy(() -> new RiskModelConfiguration().riskModel(JSON,
					new ByteArrayResource(tampered.getBytes(StandardCharsets.UTF_8)), genuineSha256))
			.withMessageContaining("integrity check failed");
	}

	@Test
	void aModelWithOtherFeaturesIsRefused() throws Exception {
		String renamed = MODEL_FILE.getContentAsString(StandardCharsets.UTF_8).replace("new_device", "new_browser");
		byte[] content = renamed.getBytes(StandardCharsets.UTF_8);
		assertThatIllegalStateException()
			.isThrownBy(() -> new RiskModelConfiguration().riskModel(JSON, new ByteArrayResource(content),
					RiskModelConfiguration.sha256(content)))
			.withMessageContaining("do not match");
	}

}
