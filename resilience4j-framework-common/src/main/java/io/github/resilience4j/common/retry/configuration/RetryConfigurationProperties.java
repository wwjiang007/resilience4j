package io.github.resilience4j.common.retry.configuration;
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

import io.github.resilience4j.core.ClassUtils;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Main spring properties for retry configuration
 */
public class RetryConfigurationProperties {

	private final Map<String, InstanceProperties> instances = new HashMap<>();
	private Map<String, InstanceProperties> configs = new HashMap<>();

	/**
	 * @param backend backend name
	 * @return the retry configuration
	 */
	public RetryConfig createRetryConfig(String backend) {
		return createRetryConfig(getBackendProperties(backend));
	}

	/**
	 * @param backend retry backend name
	 * @return the configured spring backend properties
	 */
	@Nullable
	public InstanceProperties getBackendProperties(String backend) {
		return instances.get(backend);
	}

	/**
	 * @return the configured retry backend properties
	 */
	public Map<String, InstanceProperties> getInstances() {
		return instances;
	}

	/**
	 * For backwards compatibility when setting backends in configuration properties.
	 */
	public Map<String, InstanceProperties> getBackends() {
		return instances;
	}

	/**
	 * @return common configuration for retry backend
	 */
	public Map<String, InstanceProperties> getConfigs() {
		return configs;
	}

	/**
	 * @param instanceProperties the retry backend spring properties
	 * @return the retry configuration
	 */
	public RetryConfig createRetryConfig(InstanceProperties instanceProperties) {
		if (StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
			InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
			if (baseProperties == null) {
				throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
			}
			return buildConfigFromBaseConfig(baseProperties, instanceProperties);
		}
		return buildRetryConfig(RetryConfig.custom(), instanceProperties);
	}

	private RetryConfig buildConfigFromBaseConfig(InstanceProperties baseProperties, InstanceProperties instanceProperties) {
		RetryConfig baseConfig = buildRetryConfig(RetryConfig.custom(), baseProperties);
		return buildRetryConfig(RetryConfig.from(baseConfig), instanceProperties);
	}

	/**
	 * @param properties the configured spring backend properties
	 * @return retry config builder instance
	 */
	@SuppressWarnings("unchecked")
	private RetryConfig buildRetryConfig(RetryConfig.Builder builder, InstanceProperties properties) {
		if (properties == null) {
			return builder.build();
		}

		if (properties.enableExponentialBackoff && properties.enableRandomizedWait) {
			throw new IllegalStateException("you can not enable Exponential backoff policy and randomized delay at the same time , please enable only one of them");
		}

		configureRetryIntervalFunction(properties, builder);

		if (properties.getMaxRetryAttempts() != null && properties.getMaxRetryAttempts() != 0) {
			builder.maxAttempts(properties.getMaxRetryAttempts());
		}

		if (properties.getRetryExceptionPredicate() != null) {
			if (properties.getRetryExceptionPredicate() != null) {
				Predicate<Throwable> predicate = ClassUtils.instantiatePredicateClass(properties.getRetryExceptionPredicate());
				if (predicate != null) {
					builder.retryOnException(predicate);
				}
			}
		}

		if (properties.getIgnoreExceptions() != null) {
			builder.ignoreExceptions(properties.getIgnoreExceptions());
		}

		if (properties.getRetryExceptions() != null) {
			builder.retryExceptions(properties.getRetryExceptions());
		}

		if (properties.getResultPredicate() != null) {
			if (properties.getResultPredicate() != null) {
				Predicate<Object> predicate = ClassUtils.instantiatePredicateClass(properties.getResultPredicate());
				if (predicate != null) {
					builder.retryOnResult(predicate);
				}
			}
		}

		return builder.build();
	}

	/**
	 * decide which retry delay policy will be configured based into the configured properties
	 *
	 * @param properties the backend retry properties
	 * @param builder    the retry config builder
	 */
	private void configureRetryIntervalFunction(InstanceProperties properties, RetryConfig.Builder<Object> builder) {
		if (properties.getWaitDurationMillis() != null && properties.getWaitDurationMillis() != 0) {
			long waitDuration = properties.getWaitDurationMillis();
			if (properties.getEnableExponentialBackoff()) {
				if (properties.getExponentialBackoffMultiplier() != 0) {
					builder.intervalFunction(IntervalFunction.ofExponentialBackoff(waitDuration, properties.getExponentialBackoffMultiplier()));
				} else {
					builder.intervalFunction(IntervalFunction.ofExponentialBackoff(properties.getWaitDurationMillis()));
				}
			} else if (properties.getEnableRandomizedWait()) {
				if (properties.getRandomizedWaitFactor() != 0) {
					builder.intervalFunction(IntervalFunction.ofRandomized(waitDuration, properties.getRandomizedWaitFactor()));
				} else {
					builder.intervalFunction(IntervalFunction.ofRandomized(waitDuration));
				}
			} else {
				builder.waitDuration(Duration.ofMillis(properties.getWaitDurationMillis()));
			}
		}
	}

	/**
	 * Class storing property values for configuring {@link io.github.resilience4j.retry.Retry} instances.
	 */
	public static class InstanceProperties {
		/*
		 * wait long value for the next try
		 */
		@Min(100)
		@Nullable
		private Long waitDurationMillis;
		/*
		 * max retry attempts value
		 */
		@Min(1)
		@Nullable
		private Integer maxRetryAttempts;
		/*
		 * retry exception predicate class to be used to evaluate the exception to retry or not
		 */
		@Nullable
		private Class<? extends Predicate<Throwable>> retryExceptionPredicate;
		/*
		 * retry setResultPredicate predicate class to be used to evaluate the result to retry or not
		 */
		@Nullable
		private Class<? extends Predicate<Object>> resultPredicate;
		/*
		 * list of retry exception classes
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		private Class<? extends Throwable>[] retryExceptions;
		/*
		 * list of retry ignored exception classes
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		private Class<? extends Throwable>[] ignoreExceptions;
		/*
		 * event buffer size for generated retry events
		 */
		@Min(1)
		private Integer eventConsumerBufferSize = 100;
		/*
		 * flag to enable Exponential backoff policy or not for retry policy delay
		 */
		@NotNull
		private Boolean enableExponentialBackoff = false;
		/*
		 * exponential backoff multiplier value
		 */
		private double exponentialBackoffMultiplier;
		@NotNull
		/*
		 * flag to enable randomized delay  policy or not for retry policy delay
		 */
		private Boolean enableRandomizedWait = false;
		/*
		 * randomized delay factor value
		 */
		private double randomizedWaitFactor;

		@Nullable
		private String baseConfig;

		@Nullable
		public Long getWaitDurationMillis() {
			return waitDurationMillis;
		}

		public InstanceProperties setWaitDurationMillis(Long waitDurationMillis) {
			this.waitDurationMillis = waitDurationMillis;
			return this;
		}

		@Nullable
		public Integer getMaxRetryAttempts() {
			return maxRetryAttempts;
		}

		public InstanceProperties setMaxRetryAttempts(Integer maxRetryAttempts) {
			this.maxRetryAttempts = maxRetryAttempts;
			return this;
		}

		@Nullable
		public Class<? extends Predicate<Throwable>> getRetryExceptionPredicate() {
			return retryExceptionPredicate;
		}

		public InstanceProperties setRetryExceptionPredicate(Class<? extends Predicate<Throwable>> retryExceptionPredicate) {
			this.retryExceptionPredicate = retryExceptionPredicate;
			return this;
		}

		@Nullable
		public Class<? extends Predicate<Object>> getResultPredicate() {
			return resultPredicate;
		}

		public InstanceProperties setResultPredicate(Class<? extends Predicate<Object>> resultPredicate) {
			this.resultPredicate = resultPredicate;
			return this;
		}

		@Nullable
		public Class<? extends Throwable>[] getRetryExceptions() {
			return retryExceptions;
		}

		public InstanceProperties setRetryExceptions(Class<? extends Throwable>[] retryExceptions) {
			this.retryExceptions = retryExceptions;
			return this;
		}

		@Nullable
		public Class<? extends Throwable>[] getIgnoreExceptions() {
			return ignoreExceptions;
		}

		public InstanceProperties setIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
			this.ignoreExceptions = ignoreExceptions;
			return this;
		}

		@Nullable
		public Integer getEventConsumerBufferSize() {
			return eventConsumerBufferSize;
		}

		public InstanceProperties setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
			this.eventConsumerBufferSize = eventConsumerBufferSize;
			return this;
		}

		public Boolean getEnableExponentialBackoff() {
			return enableExponentialBackoff;
		}

		public InstanceProperties setEnableExponentialBackoff(Boolean enableExponentialBackoff) {
			this.enableExponentialBackoff = enableExponentialBackoff;
			return this;
		}

		@Nullable
		public double getExponentialBackoffMultiplier() {
			return exponentialBackoffMultiplier;
		}

		public InstanceProperties setExponentialBackoffMultiplier(double exponentialBackoffMultiplier) {
			this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
			return this;
		}

		@Nullable
		public Boolean getEnableRandomizedWait() {
			return enableRandomizedWait;
		}

		public InstanceProperties setEnableRandomizedWait(Boolean enableRandomizedWait) {
			this.enableRandomizedWait = enableRandomizedWait;
			return this;
		}

		@Nullable
		public double getRandomizedWaitFactor() {
			return randomizedWaitFactor;
		}

		public InstanceProperties setRandomizedWaitFactor(double randomizedWaitFactor) {
			this.randomizedWaitFactor = randomizedWaitFactor;
			return this;
		}

		/**
		 * Gets the shared configuration name. If this is set, the configuration builder will use the the shared
		 * configuration backend over this one.
		 *
		 * @return The shared configuration name.
		 */
		@Nullable
		public String getBaseConfig() {
			return baseConfig;
		}

		/**
		 * Sets the shared configuration name. If this is set, the configuration builder will use the the shared
		 * configuration backend over this one.
		 *
		 * @param baseConfig The shared configuration name.
		 */
		public InstanceProperties setBaseConfig(String baseConfig) {
			this.baseConfig = baseConfig;
			return this;
		}

	}

}
