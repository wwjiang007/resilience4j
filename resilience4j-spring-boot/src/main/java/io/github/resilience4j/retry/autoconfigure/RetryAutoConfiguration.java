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
package io.github.resilience4j.retry.autoconfigure;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.monitoring.endpoint.RetryEndpoint;
import io.github.resilience4j.retry.monitoring.endpoint.RetryEventsEndpoint;


/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-retry.
 */
@Configuration
@ConditionalOnClass(Retry.class)
@EnableConfigurationProperties(RetryProperties.class)
@Import(RetryConfigurationOnMissingBean.class)
@AutoConfigureBefore(EndpointAutoConfiguration.class)
public class RetryAutoConfiguration {

	@Bean
	public RetryEndpoint retryEndpoint(RetryRegistry retryRegistry) {
		return new RetryEndpoint(retryRegistry);
	}

	@Bean
	public RetryEventsEndpoint retryEventsEndpoint(EventConsumerRegistry<RetryEvent> eventConsumerRegistry) {
		return new RetryEventsEndpoint(eventConsumerRegistry);
	}

}
