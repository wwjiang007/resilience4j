/*
 * Copyright 2019 Yevhenii Voievodin, Robert Winkler
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
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/** Collects circuit breaker exposed {@link Metrics}. */
public class CircuitBreakerMetricsCollector extends Collector {

    private static final String KIND_FAILED = "failed";
    private static final String KIND_SUCCESSFUL = "successful";
    private static final String KIND_IGNORED = "ignored";
    private static final String KIND_NOT_PERMITTED = "not_permitted";

    private static final List<String> NAME_AND_STATE = Arrays.asList("name", "state");

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of circuit breakers.
     *
     * @param names    the custom metric names
     * @param circuitBreakerRegistry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(MetricNames names, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetricsCollector(names, circuitBreakerRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of circuit breakers.
     *
     * @param circuitBreakerRegistry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetricsCollector(MetricNames.ofDefaults(), circuitBreakerRegistry);
    }

    private final MetricNames names;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CollectorRegistry collectorRegistry = new CollectorRegistry(true);
    private final Histogram callsHistogram;

    private CircuitBreakerMetricsCollector(MetricNames names, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.names = requireNonNull(names);
        this.circuitBreakerRegistry = requireNonNull(circuitBreakerRegistry);

        callsHistogram = Histogram.build(names.getCallsMetricName(), "Total number of calls by kind")
                .labelNames("name", "kind")
                .create().register(collectorRegistry);

        for (CircuitBreaker circuitBreaker : this.circuitBreakerRegistry.getAllCircuitBreakers()) {
            addMetrics(circuitBreaker);
        }
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(event -> addMetrics(event.getAddedEntry()));
    }

    private void addMetrics(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_NOT_PERMITTED).observe(0))
                .onIgnoredError(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_IGNORED).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
                .onSuccess(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_SUCCESSFUL).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
                .onError(event -> callsHistogram.labels(circuitBreaker.getName(), KIND_FAILED).observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND));
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = Collections.list(collectorRegistry.metricFamilySamples());
        samples.addAll(collectGaugeSamples());
        return samples;
    }

    private List<MetricFamilySamples> collectGaugeSamples() {
        GaugeMetricFamily stateFamily = new GaugeMetricFamily(
                names.getStateMetricName(),
                "The state of the circuit breaker:",
                NAME_AND_STATE
        );
        GaugeMetricFamily bufferedCallsFamily = new GaugeMetricFamily(
                names.getBufferedCallsMetricName(),
                "The number of buffered calls",
                LabelNames.NAME_AND_KIND
        );
        GaugeMetricFamily maxBufferedCallsFamily = new GaugeMetricFamily(
                names.getMaxBufferedCallsMetricName(),
                "The maximum number of buffered calls",
                LabelNames.NAME
        );

        GaugeMetricFamily failureRateFamily = new GaugeMetricFamily(
                names.getFailureRateMetricName(),
                "The failure rate",
                LabelNames.NAME
        );

        for (CircuitBreaker circuitBreaker : this.circuitBreakerRegistry.getAllCircuitBreakers()) {
            final CircuitBreaker.State[] states = CircuitBreaker.State.values();
            for (CircuitBreaker.State state : states) {
                stateFamily.addMetric(asList(circuitBreaker.getName(), state.name().toLowerCase()),
                        circuitBreaker.getState() == state ? 1 : 0);
            }

            List<String> nameLabel = Collections.singletonList(circuitBreaker.getName());
            Metrics metrics = circuitBreaker.getMetrics();
            bufferedCallsFamily.addMetric(asList(circuitBreaker.getName(), KIND_SUCCESSFUL), metrics.getNumberOfSuccessfulCalls());
            bufferedCallsFamily.addMetric(asList(circuitBreaker.getName(), KIND_FAILED), metrics.getNumberOfFailedCalls());
            maxBufferedCallsFamily.addMetric(nameLabel, metrics.getMaxNumberOfBufferedCalls());
            failureRateFamily.addMetric(nameLabel, metrics.getFailureRate());
        }
        return asList(stateFamily, bufferedCallsFamily, maxBufferedCallsFamily, failureRateFamily);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_CIRCUIT_BREAKER_CALLS = "resilience4j_circuitbreaker_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_STATE = "resilience4j_circuitbreaker_state";
        public static final String DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS = "resilience4j_circuitbreaker_buffered_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS = "resilience4j_circuitbreaker_max_buffered_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE = "resilience4j_circuitbreaker_failure_rate";

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
         */
        public static Builder custom() {
            return new Builder();
        }

        /** Returns default metric names. */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        private String callsMetricName = DEFAULT_CIRCUIT_BREAKER_CALLS;
        private String stateMetricName = DEFAULT_CIRCUIT_BREAKER_STATE;
        private String bufferedCallsMetricName = DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS;
        private String maxBufferedCallsMetricName = DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS;
        private String failureRateMetricName = DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE;

        private MetricNames() {}

        /** Returns the metric name for circuit breaker calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_CALLS}. */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /** Returns the metric name for currently buffered calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS}. */
        public String getBufferedCallsMetricName() {
            return bufferedCallsMetricName;
        }

        /** Returns the metric name for max buffered calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS}. */
        public String getMaxBufferedCallsMetricName() {
            return maxBufferedCallsMetricName;
        }

        /** Returns the metric name for failure rate, defaults to {@value DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE}. */
        public String getFailureRateMetricName() {
            return failureRateMetricName;
        }

        /** Returns the metric name for state, defaults to {@value DEFAULT_CIRCUIT_BREAKER_STATE}. */
        public String getStateMetricName() {
            return stateMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {
            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_CALLS} with a given one. */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_STATE} with a given one. */
            public Builder stateMetricName(String stateMetricName) {
                metricNames.stateMetricName = requireNonNull(stateMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS} with a given one. */
            public Builder bufferedCallsMetricName(String bufferedCallsMetricName) {
                metricNames.bufferedCallsMetricName = requireNonNull(bufferedCallsMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS} with a given one. */
            public Builder maxBufferedCallsMetricName(String maxBufferedCallsMetricName) {
                metricNames.maxBufferedCallsMetricName = requireNonNull(maxBufferedCallsMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE} with a given one. */
            public Builder failureRateMetricName(String failureRateMetricName) {
                metricNames.failureRateMetricName = requireNonNull(failureRateMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance. */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
