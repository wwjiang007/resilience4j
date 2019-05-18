/*
 *
 *  Copyright 2017 Robert Winkler, Mahmoud Romeh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.assertThat;


public class ThreadPoolBulkheadRegistryTest {

	private ThreadPoolBulkheadConfig config;
	private ThreadPoolBulkheadRegistry registry;

	@Before
	public void setUp() {

		// registry with default config
		registry = ThreadPoolBulkheadRegistry.ofDefaults();

		// registry with custom config
		config = ThreadPoolBulkheadConfig.custom()
				.maxThreadPoolSize(100)
				.build();
	}

	@Test
	public void shouldReturnCustomConfig() {

		// give
		ThreadPoolBulkheadRegistry registry = ThreadPoolBulkheadRegistry.of(config);

		// when
		ThreadPoolBulkheadConfig bulkheadConfig = registry.getDefaultConfig();

		// then
		assertThat(bulkheadConfig).isSameAs(config);
	}

	@Test
	public void shouldReturnTheCorrectName() {

		ThreadPoolBulkhead bulkhead = registry.bulkhead("test");

		assertThat(bulkhead).isNotNull();
		assertThat(bulkhead.getName()).isEqualTo("test");
		assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(Runtime.getRuntime().availableProcessors());
	}

	@Test
	public void shouldBeTheSameInstance() {

		ThreadPoolBulkhead bulkhead1 = registry.bulkhead("test", config);
		ThreadPoolBulkhead bulkhead2 = registry.bulkhead("test", config);

		assertThat(bulkhead1).isSameAs(bulkhead2);
		assertThat(registry.getAllBulkheads()).hasSize(1);
	}

	@Test
	public void shouldBeNotTheSameInstance() {

		ThreadPoolBulkhead bulkhead1 = registry.bulkhead("test1");
		ThreadPoolBulkhead bulkhead2 = registry.bulkhead("test2");

		assertThat(bulkhead1).isNotSameAs(bulkhead2);
		assertThat(registry.getAllBulkheads()).hasSize(2);
	}

	@Test
	public void testCreateWithConfigurationMap() {
		Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
		configs.put("default", ThreadPoolBulkheadConfig.ofDefaults());
		configs.put("custom", ThreadPoolBulkheadConfig.ofDefaults());

		ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry.of(configs);

		assertThat(threadPoolBulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(threadPoolBulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testCreateWithConfigurationMapWithoutDefaultConfig() {
		Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
		configs.put("custom", ThreadPoolBulkheadConfig.ofDefaults());

		ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry.of(configs);

		assertThat(threadPoolBulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(threadPoolBulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testWithNotExistingConfig() {
		ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry.ofDefaults();

		assertThatThrownBy(() -> threadPoolBulkheadRegistry.bulkhead("test", "doesNotExist"))
				.isInstanceOf(ConfigurationNotFoundException.class);
	}

	@Test
	public void testAddConfiguration() {
		ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry.ofDefaults();
		threadPoolBulkheadRegistry.addConfiguration("custom", ThreadPoolBulkheadConfig.custom().build());

		assertThat(threadPoolBulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(threadPoolBulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

}
