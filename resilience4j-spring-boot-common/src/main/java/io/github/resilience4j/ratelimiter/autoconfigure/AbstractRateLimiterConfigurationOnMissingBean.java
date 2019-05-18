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
package io.github.resilience4j.ratelimiter.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspect;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspectExt;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfiguration;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.configure.ReactorRateLimiterAspectExt;
import io.github.resilience4j.ratelimiter.configure.RxJava2RateLimiterAspectExt;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;

@Configuration
@Import(FallbackConfigurationOnMissingBean.class)
public abstract class AbstractRateLimiterConfigurationOnMissingBean {
	protected final RateLimiterConfiguration rateLimiterConfiguration;

	public AbstractRateLimiterConfigurationOnMissingBean() {
		this.rateLimiterConfiguration = new RateLimiterConfiguration();
	}

	@Bean
	@ConditionalOnMissingBean
	public RateLimiterRegistry rateLimiterRegistry(RateLimiterConfigurationProperties rateLimiterProperties,
	                                               EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry) {
		return rateLimiterConfiguration.rateLimiterRegistry(rateLimiterProperties, rateLimiterEventsConsumerRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	public RateLimiterAspect rateLimiterAspect(RateLimiterConfigurationProperties rateLimiterProperties, RateLimiterRegistry rateLimiterRegistry, @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList, FallbackDecorators fallbackDecorators) {
		return rateLimiterConfiguration.rateLimiterAspect(rateLimiterProperties, rateLimiterRegistry, rateLimiterAspectExtList, fallbackDecorators);
	}

	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	@ConditionalOnMissingBean
	public RxJava2RateLimiterAspectExt rxJava2RateLimterAspectExt() {
		return rateLimiterConfiguration.rxJava2RateLimterAspectExt();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	@ConditionalOnMissingBean
	public ReactorRateLimiterAspectExt reactorRateLimiterAspectExt() {
		return rateLimiterConfiguration.reactorRateLimiterAspectExt();
	}

}
