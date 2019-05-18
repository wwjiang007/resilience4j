/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.ratelimiter.internal.InMemoryRateLimiterRegistry;
import io.vavr.collection.Seq;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages all RateLimiter instances.
 */
public interface RateLimiterRegistry extends Registry<RateLimiter, RateLimiterConfig> {

    /**
     * Returns all managed {@link RateLimiter} instances.
     *
     * @return all managed {@link RateLimiter} instances.
     */
    Seq<RateLimiter> getAllRateLimiters();

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter configuration.
     *
     * @param name the name of the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RateLimiterConfig rateLimiterConfig);

    /**
     * Returns a managed {@link RateLimiterConfig} or creates a new one with a custom RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @return The {@link RateLimiterConfig}
     */
    RateLimiter rateLimiter(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter configuration.
     *
     * @param name       the name of the RateLimiter
     * @param configName a custom RateLimiter configuration name
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, String configName);

    /**
     * Creates a RateLimiterRegistry with a custom RateLimiter configuration.
     *
     * @param defaultRateLimiterConfig a custom RateLimiter configuration
     * @return a RateLimiterRegistry instance backed by a custom RateLimiter configuration
     */
    static RateLimiterRegistry of(RateLimiterConfig defaultRateLimiterConfig) {
        return new InMemoryRateLimiterRegistry(defaultRateLimiterConfig);
    }

    /**
     * Returns a managed {@link RateLimiterConfig} or creates a new one with a default RateLimiter configuration.
     *
     * @return The {@link RateLimiterConfig}
     */
    static RateLimiterRegistry ofDefaults() {
        return new InMemoryRateLimiterRegistry(RateLimiterConfig.ofDefaults());
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     *
     * @param configs a Map of shared RateLimiter configurations
     * @return a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     */
    static RateLimiterRegistry of(Map<String, RateLimiterConfig> configs) {
        return new InMemoryRateLimiterRegistry(configs);
    }
}
