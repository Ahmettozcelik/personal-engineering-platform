package com.my.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class HealthGroupsTests {

	@Autowired
	private HealthEndpointGroups groups;

	@Test
	void readinessIncludesDatabase() {
		HealthEndpointGroup readiness = groups.get("readiness");

		assertThat(readiness.isMember("readinessState")).isTrue();
		assertThat(readiness.isMember("db")).isTrue();
	}

	@Test
	void livenessDoesNotDependOnDatabase() {
		HealthEndpointGroup liveness = groups.get("liveness");

		assertThat(liveness.isMember("livenessState")).isTrue();
		assertThat(liveness.isMember("db")).isFalse();
	}

}
