package io.github.resilience4j.retry.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of retry configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class RetryConfigurationTest {

	@Test
	public void testRetryRegistry() {
		//Given
		io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties instanceProperties1 = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
		instanceProperties1.setMaxRetryAttempts(3);

		io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties instanceProperties2 = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
		instanceProperties2.setMaxRetryAttempts(2);

		RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
		retryConfigurationProperties.getInstances().put("backend1", instanceProperties1);
		retryConfigurationProperties.getInstances().put("backend2", instanceProperties2);

		RetryConfiguration retryConfiguration = new RetryConfiguration();
		DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		RetryRegistry retryRegistry = retryConfiguration.retryRegistry(retryConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(retryRegistry.getAllRetries().size()).isEqualTo(2);
		Retry retry1 = retryRegistry.retry("backend1");
		assertThat(retry1).isNotNull();
		assertThat(retry1.getRetryConfig().getMaxAttempts()).isEqualTo(3);

		Retry retry2 = retryRegistry.retry("backend2");
		assertThat(retry2).isNotNull();
		assertThat(retry2.getRetryConfig().getMaxAttempts()).isEqualTo(2);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateRetryRegistryWithSharedConfigs() {
		//Given
		io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties defaultProperties = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
		defaultProperties.setMaxRetryAttempts(3);
		defaultProperties.setWaitDurationMillis(50L);

		io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
		sharedProperties.setMaxRetryAttempts(2);
		sharedProperties.setWaitDurationMillis(100L);

		io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setWaitDurationMillis(200L);

		io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties backendWithSharedConfig = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setWaitDurationMillis(300L);

		RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
		retryConfigurationProperties.getConfigs().put("default", defaultProperties);
		retryConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		retryConfigurationProperties.getInstances().put("backendWithDefaultConfig", backendWithDefaultConfig);
		retryConfigurationProperties.getInstances().put("backendWithSharedConfig", backendWithSharedConfig);

		RetryConfiguration retryConfiguration = new RetryConfiguration();
		DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		RetryRegistry retryRegistry = retryConfiguration.retryRegistry(retryConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(retryRegistry.getAllRetries().size()).isEqualTo(2);

		// Should get default config and overwrite max attempt and wait time
		Retry retry1 = retryRegistry.retry("backendWithDefaultConfig");
		assertThat(retry1).isNotNull();
		assertThat(retry1.getRetryConfig().getMaxAttempts()).isEqualTo(3);
		assertThat(retry1.getRetryConfig().getIntervalFunction().apply(1)).isEqualTo(200L);

		// Should get shared config and overwrite wait time
		Retry retry2 = retryRegistry.retry("backendWithSharedConfig");
		assertThat(retry2).isNotNull();
		assertThat(retry2.getRetryConfig().getMaxAttempts()).isEqualTo(2);
		assertThat(retry2.getRetryConfig().getIntervalFunction().apply(1)).isEqualTo(300L);

		// Unknown backend should get default config of Registry
		Retry retry3 = retryRegistry.retry("unknownBackend");
		assertThat(retry3).isNotNull();
		assertThat(retry3.getRetryConfig().getMaxAttempts()).isEqualTo(3);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
	}

	@Test
	public void testCreateRetryRegistryWithUnknownConfig() {
		RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();

		io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties instanceProperties = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
		instanceProperties.setBaseConfig("unknownConfig");
		retryConfigurationProperties.getInstances().put("backend", instanceProperties);

		RetryConfiguration retryConfiguration = new RetryConfiguration();
		DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		assertThatThrownBy(() -> retryConfiguration.retryRegistry(retryConfigurationProperties, eventConsumerRegistry))
				.isInstanceOf(ConfigurationNotFoundException.class)
				.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}