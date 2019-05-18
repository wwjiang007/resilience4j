/*
 * Copyright 2019 lespinsideg
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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.prometheus.collectors.BulkheadMetricsCollector;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-metrics.
 */
@Configuration
@AutoConfigureAfter(value = BulkheadAutoConfiguration.class)
@ConditionalOnClass(BulkheadMetricsCollector.class)
@ConditionalOnProperty(value = "resilience4j.bulkhead.metrics.enabled", matchIfMissing = true)
public class BulkheadPrometheusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BulkheadMetricsCollector bulkheadPrometheusCollector(BulkheadRegistry bulkheadRegistry) {
        BulkheadMetricsCollector collector = BulkheadMetricsCollector.ofBulkheadRegistry(bulkheadRegistry);
        collector.register();
        return collector;
    }
}
