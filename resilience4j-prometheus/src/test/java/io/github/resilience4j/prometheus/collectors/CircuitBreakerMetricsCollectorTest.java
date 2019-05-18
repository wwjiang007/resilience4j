/*
 * Copyright 2019 Yevhenii Voievodin
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
package io.github.resilience4j.prometheus.collectors;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import static io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector.MetricNames.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerMetricsCollectorTest {

    CollectorRegistry registry;
    CircuitBreaker circuitBreaker;
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Before
    public void setup() {
        registry = new CollectorRegistry();
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("backendA");

        CircuitBreakerMetricsCollector.ofCircuitBreakerRegistry(circuitBreakerRegistry).register(registry);

        // record some basic stats
        circuitBreaker.onSuccess(0);
        circuitBreaker.onError(0, new RuntimeException("oops"));
        circuitBreaker.transitionToOpenState();
    }

    @Test
    public void stateReportsCorrespondingValue() {
        double state = registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_STATE,
            new String[]{"name", "state"},
            new String[]{circuitBreaker.getName(), circuitBreaker.getState().name().toLowerCase()}
        );

        assertThat(state).isEqualTo(1);
    }

    @Test
    public void shouldRemoveCircuitBreakerMetrics() {
        double state = registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_STATE,
                new String[]{"name", "state"},
                new String[]{circuitBreaker.getName(), circuitBreaker.getState().name().toLowerCase()}
        );

        assertThat(state).isEqualTo(1);

        circuitBreakerRegistry.remove(circuitBreaker.getName());

        assertThat(registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_STATE,
                new String[]{"name", "state"},
                new String[]{circuitBreaker.getName(), circuitBreaker.getState().name().toLowerCase()}
        )).isNull();
    }

    @Test
    public void shouldReportNewlyAddedCircuitBreaker() {
        String name = "newBackend";
        assertThat(registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_STATE,
                new String[]{"name", "state"},
                new String[]{name, circuitBreaker.getState().name().toLowerCase()}
        )).isNull();

        circuitBreakerRegistry.circuitBreaker(name);

        double state = registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_STATE,
                new String[]{"name", "state"},
                new String[]{circuitBreaker.getName(), circuitBreaker.getState().name().toLowerCase()}
        );

        assertThat(state).isEqualTo(1);
    }

    @Test
    public void maxBufferedCallsReportsCorrespondingValue() {
        double maxBufferedCalls = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS,
            new String[]{"name"},
            new String[]{circuitBreaker.getName()}
        );

        assertThat(maxBufferedCalls).isEqualTo(circuitBreaker.getMetrics().getMaxNumberOfBufferedCalls());
    }

    @Test
    public void successfulBufferedCallsReportsCorrespondingValue() {
        double successfulCalls = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS,
            new String[]{"name", "kind"},
            new String[]{circuitBreaker.getName(), "successful"}
        );

        assertThat(successfulCalls).isEqualTo(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    public void failedBufferedCallsReportsCorrespondingValue() {
        double failedCalls = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS,
            new String[]{"name", "kind"},
            new String[]{circuitBreaker.getName(), "failed"}
        );

        assertThat(failedCalls).isEqualTo(circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    public void successfulCallsBucketReportsCorrespondingValue() {
        double successfulCalls = registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_CALLS + "_bucket",
                new String[]{"name", "kind", "le"},
                new String[]{circuitBreaker.getName(), "successful", "0.1"}
        );

        assertThat(successfulCalls).isEqualTo(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    public void failedCallsBucketReportsCorrespondingValue() {
        double failedCalls = registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_CALLS + "_bucket",
                new String[]{"name", "kind", "le"},
                new String[]{circuitBreaker.getName(), "failed", "0.1"}
        );

        assertThat(failedCalls).isEqualTo(circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    public void notPermittedCallsBucketReportsCorrespondingValue() {
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();
        double notPermitted = registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_CALLS + "_bucket",
            new String[]{"name", "kind", "le"},
            new String[]{circuitBreaker.getName(), "not_permitted", "0.1"}
        );

        assertThat(notPermitted).isEqualTo(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
    }

    @Test
    public void failureRateReportsCorrespondingValue() {
        Double failureRate = registry.getSampleValue(
                DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE,
                new String[]{"name"},
                new String[]{circuitBreaker.getName()}
        );

        assertThat(failureRate.floatValue()).isEqualTo(circuitBreaker.getMetrics().getFailureRate());
    }

    @Test
    public void customMetricNamesOverrideDefaultOnes() {
        CollectorRegistry registry = new CollectorRegistry();

        CircuitBreakerMetricsCollector.ofCircuitBreakerRegistry(
            CircuitBreakerMetricsCollector.MetricNames.custom()
                .callsMetricName("custom_calls")
                .stateMetricName("custom_state")
                .maxBufferedCallsMetricName("custom_max_buffered_calls")
                .bufferedCallsMetricName("custom_buffered_calls")
                .failureRateMetricName("custom_failure_rate")
                .build(),
            circuitBreakerRegistry).register(registry);

        assertThat(registry.getSampleValue(
            "custom_buffered_calls",
            new String[]{"name", "kind"},
            new String[]{"backendA", "successful"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_buffered_calls",
            new String[]{"name", "kind"},
            new String[]{"backendA", "failed"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_state",
            new String[]{"name", "state"},
            new String[]{"backendA", "closed"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_max_buffered_calls",
            new String[]{"name"},
            new String[]{"backendA"}
        )).isNotNull();
    }
}
