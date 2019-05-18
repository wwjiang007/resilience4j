/*
 *
 *  Copyright 2019 Robert Winkler
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
package io.github.resilience4j.bulkhead.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;

public class FixedThreadPoolBulkheadTest {

	private ThreadPoolBulkhead bulkhead;

	@Before
	public void setUp() {

		ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
				.maxThreadPoolSize(2)
				.coreThreadPoolSize(1)
				.queueCapacity(10)
				.keepAliveTime(10)
				.build();

		bulkhead = ThreadPoolBulkhead.of("test", config);
	}

	@Test
	public void testToString() {

		// when
		String result = bulkhead.toString();

		// then
		assertThat(result).isEqualTo("FixedThreadPoolBulkhead 'test'");
	}

	@Test
	public void testCustomSettings() {

		// then
		assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(2);
		assertThat(bulkhead.getBulkheadConfig().getQueueCapacity()).isEqualTo(10);
		assertThat(bulkhead.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(1);
		assertThat(bulkhead.getBulkheadConfig().getKeepAliveTime()).isEqualTo(10);
	}

	@Test
	public void testCreateWithDefaults() {
		// when
		ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("test");

		// then
		assertThat(bulkhead).isNotNull();
		assertThat(bulkhead.getBulkheadConfig()).isNotNull();
		assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE);
		assertThat(bulkhead.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_CORE_THREAD_POOL_SIZE);
		assertThat(bulkhead.getBulkheadConfig().getQueueCapacity()).isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_QUEUE_CAPACITY);
	}


}
