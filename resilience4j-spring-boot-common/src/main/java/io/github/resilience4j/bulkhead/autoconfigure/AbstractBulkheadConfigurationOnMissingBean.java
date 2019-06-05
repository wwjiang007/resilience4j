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
package io.github.resilience4j.bulkhead.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.BulkheadAspect;
import io.github.resilience4j.bulkhead.configure.BulkheadAspectExt;
import io.github.resilience4j.bulkhead.configure.BulkheadConfiguration;
import io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.bulkhead.configure.ReactorBulkheadAspectExt;
import io.github.resilience4j.bulkhead.configure.RxJava2BulkheadAspectExt;
import io.github.resilience4j.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import io.github.resilience4j.bulkhead.configure.threadpool.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;

/**
 * {@link Configuration
 * Configuration} for resilience4j-bulkhead.
 */
@Configuration
@Import(FallbackConfigurationOnMissingBean.class)
public abstract class AbstractBulkheadConfigurationOnMissingBean {

	protected final BulkheadConfiguration bulkheadConfiguration;
	protected final ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration;

	public AbstractBulkheadConfigurationOnMissingBean() {
		this.threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();
		this.bulkheadConfiguration = new BulkheadConfiguration();
	}

	@Bean
	@ConditionalOnMissingBean
	public BulkheadRegistry bulkheadRegistry(BulkheadConfigurationProperties bulkheadConfigurationProperties,
	                                         EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry) {
		return bulkheadConfiguration.bulkheadRegistry(bulkheadConfigurationProperties, bulkheadEventConsumerRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	public BulkheadAspect bulkheadAspect(BulkheadConfigurationProperties bulkheadConfigurationProperties, ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
	                                     BulkheadRegistry bulkheadRegistry, @Autowired(required = false) List<BulkheadAspectExt> bulkHeadAspectExtList,
	                                     FallbackDecorators fallbackDecorators) {
		return bulkheadConfiguration.bulkheadAspect(bulkheadConfigurationProperties, threadPoolBulkheadRegistry, bulkheadRegistry, bulkHeadAspectExtList, fallbackDecorators);
	}

	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	@ConditionalOnMissingBean
	public RxJava2BulkheadAspectExt rxJava2BulkHeadAspectExt() {
		return bulkheadConfiguration.rxJava2BulkHeadAspectExt();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	@ConditionalOnMissingBean
	public ReactorBulkheadAspectExt reactorBulkHeadAspectExt() {
		return bulkheadConfiguration.reactorBulkHeadAspectExt();
	}


	@Bean
	@ConditionalOnMissingBean
	public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry(ThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties,
	                                                             EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry) {

		return threadPoolBulkheadConfiguration.threadPoolBulkheadRegistry(threadPoolBulkheadConfigurationProperties, bulkheadEventConsumerRegistry);
	}

}
