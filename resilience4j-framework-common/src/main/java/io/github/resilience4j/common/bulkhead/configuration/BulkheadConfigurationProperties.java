/*
 * Copyright 2019 Dan Maas
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
package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;

import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

public class BulkheadConfigurationProperties {

    private Map<String, InstanceProperties> instances = new HashMap<>();
    private Map<String, InstanceProperties> configs = new HashMap<>();

    public io.github.resilience4j.bulkhead.BulkheadConfig createBulkheadConfig(InstanceProperties instanceProperties) {
        if (StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
            InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
            }
            return buildConfigFromBaseConfig(baseProperties, instanceProperties);
        }
        return buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.custom(), instanceProperties);
    }

    private io.github.resilience4j.bulkhead.BulkheadConfig buildConfigFromBaseConfig(InstanceProperties baseProperties, InstanceProperties instanceProperties) {
        io.github.resilience4j.bulkhead.BulkheadConfig baseConfig = buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.custom(), baseProperties);
        return buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.from(baseConfig), instanceProperties);
    }

    private io.github.resilience4j.bulkhead.BulkheadConfig buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.Builder builder, InstanceProperties instanceProperties) {
        if (instanceProperties.getMaxConcurrentCalls() != null) {
            builder.maxConcurrentCalls(instanceProperties.getMaxConcurrentCalls());
        }
        if (instanceProperties.getMaxWaitTime() != null) {
            builder.maxWaitTime(instanceProperties.getMaxWaitTime());
        }
        return builder.build();
    }

    @Nullable
    public InstanceProperties getBackendProperties(String backend) {
        return instances.get(backend);
    }

    public Map<String, InstanceProperties> getInstances() {
        return instances;
    }

    /**
     * For backwards compatibility when setting backends in configuration properties.
     */
    public Map<String, InstanceProperties> getBackends() {
        return instances;
    }

    public Map<String, InstanceProperties> getConfigs() {
        return configs;
    }

    /**
     * Bulkhead config adapter for integration with Ratpack. {@link #maxWaitTime} should
     * almost always be set to 0, so the compute threads would not be blocked upon execution.
     */
    public static class InstanceProperties {

        @Min(1)
        private Integer maxConcurrentCalls;
        @Min(0)
        private Long maxWaitTime;
        @Nullable
        private String baseConfig;
        @Min(1)
        private Integer eventConsumerBufferSize = 100;

        public InstanceProperties setMaxConcurrentCalls(Integer maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
            return this;
        }

        public InstanceProperties setMaxWaitTime(Long maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
            return this;
        }

        public InstanceProperties setBaseConfig(String baseConfig) {
            this.baseConfig = baseConfig;
            return this;
        }

        public InstanceProperties eventConsumerBufferSize(Integer eventConsumerBufferSize) {
            this.eventConsumerBufferSize = eventConsumerBufferSize;
            return this;
        }

        public Integer getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public Long getMaxWaitTime() {
            return maxWaitTime;
        }

        public String getBaseConfig() {
            return baseConfig;
        }

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

    }

}
