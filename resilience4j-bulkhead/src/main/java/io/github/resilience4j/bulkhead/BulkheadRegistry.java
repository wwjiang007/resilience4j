/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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


import io.github.resilience4j.bulkhead.internal.InMemoryBulkheadRegistry;
import io.github.resilience4j.core.Registry;
import io.vavr.collection.Seq;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link BulkheadRegistry} is a factory to create Bulkhead instances which stores all bulkhead instances in a registry.
 */
public interface BulkheadRegistry extends Registry<Bulkhead, BulkheadConfig> {

	/**
	 * Returns all managed {@link Bulkhead} instances.
	 *
	 * @return all managed {@link Bulkhead} instances.
	 */
	Seq<Bulkhead> getAllBulkheads();

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with default configuration.
	 *
	 * @param name the name of the Bulkhead
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom BulkheadConfig configuration.
	 *
	 * @param name           the name of the Bulkhead
	 * @param config a custom Bulkhead configuration
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, BulkheadConfig config);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead configuration.
	 *
	 * @param name                   the name of the Bulkhead
	 * @param bulkheadConfigSupplier a custom Bulkhead configuration supplier
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead configuration.
	 *
	 * @param name       the name of the Bulkhead
	 * @param configName a custom Bulkhead configuration name
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, String configName);

	/**
	 * @deprecated Use {@link BulkheadRegistry#getDefaultConfig()} instead
	 * @since (0.15.0)
	 * Returns a default BulkheadConfig instance this registry is using.
	 *
	 * @return BulkheadConfig instance
	 */
	@Deprecated
	BulkheadConfig getDefaultBulkheadConfig();

	/**
	 * Creates a BulkheadRegistry with a custom Bulkhead configuration.
	 *
	 * @param bulkheadConfig a custom Bulkhead configuration
	 * @return a BulkheadRegistry instance backed by a custom Bulkhead configuration
	 */
	static BulkheadRegistry of(BulkheadConfig bulkheadConfig) {
		return new InMemoryBulkheadRegistry(bulkheadConfig);
	}

	/**
	 * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations.
	 *
	 * @param configs a Map of shared Bulkhead configurations
	 * @return a RetryRegistry with a Map of shared Bulkhead configurations.
	 */
	static BulkheadRegistry of(Map<String, BulkheadConfig> configs) {
		return new InMemoryBulkheadRegistry(configs);
	}

	/**
	 * Creates a BulkheadRegistry with a default Bulkhead configuration
	 *
	 * @return a BulkheadRegistry instance backed by a default Bulkhead configuration
	 */
	static BulkheadRegistry ofDefaults() {
		return new InMemoryBulkheadRegistry(BulkheadConfig.ofDefaults());
	}

}
