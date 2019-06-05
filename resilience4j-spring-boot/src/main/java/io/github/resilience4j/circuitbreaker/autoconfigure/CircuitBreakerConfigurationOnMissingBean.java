/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfigurationOnMissingBean extends AbstractCircuitBreakerConfigurationOnMissingBean {

	private final ConfigurableBeanFactory beanFactory;

	public CircuitBreakerConfigurationOnMissingBean(CircuitBreakerConfigurationProperties circuitBreakerProperties,
	                                                ConfigurableBeanFactory beanFactory) {
		super(circuitBreakerProperties);
		this.beanFactory = beanFactory;
	}

	@Bean
	public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
		return circuitBreakerConfiguration.eventConsumerRegistry();
	}

	protected void createHealthIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker, CircuitBreakerConfigurationProperties circuitBreakerProperties) {
		boolean registerHealthIndicator = circuitBreakerProperties.findCircuitBreakerProperties(circuitBreaker.getName())
				.map(io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties::getRegisterHealthIndicator)
				.orElse(true);

		if(registerHealthIndicator){
			CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);
			beanFactory.registerSingleton(
					circuitBreaker.getName() + "CircuitBreakerHealthIndicator",
					healthIndicator
			);
		}
	}
}
