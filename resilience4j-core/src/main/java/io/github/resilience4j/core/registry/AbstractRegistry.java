/*
 *
 *  Copyright 2019 Mahmoud Romeh, Robert Winkler
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
package io.github.resilience4j.core.registry;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Abstract registry to be shared with all resilience4j registries
 */
public class AbstractRegistry<E, C> implements Registry<E, C> {
	protected static final String DEFAULT_CONFIG = "default";
	private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
	protected static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
	protected static final String SUPPLIER_MUST_NOT_BE_NULL = "Supplier must not be null";

	protected final ConcurrentMap<String, E> entryMap;

	protected final ConcurrentMap<String, C> configurations;

	private final RegistryEventProcessor eventProcessor;

	public AbstractRegistry(C defaultConfig) {
		this.configurations = new ConcurrentHashMap<>();
		this.entryMap = new ConcurrentHashMap<>();
		this.eventProcessor = new RegistryEventProcessor();
		this.configurations.put(DEFAULT_CONFIG, Objects.requireNonNull(defaultConfig, CONFIG_MUST_NOT_BE_NULL));
	}

	protected E computeIfAbsent(String name, Supplier<E> supplier){
		return entryMap.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL), k -> {
			E entry = supplier.get();
			eventProcessor.processEvent(new EntryAddedEvent<>(entry));
			return entry;
		});
	}

	@Override
	public Optional<E> find(String name){
		return Optional.ofNullable(entryMap.get(name));
	}

	@Override
	public Optional<E> remove(String name){
		Optional<E> removedEntry = Optional.ofNullable(entryMap.remove(name));
		removedEntry.ifPresent(entry -> eventProcessor.processEvent(new EntryRemovedEvent<>(entry)));
		return removedEntry;
	}

	@Override
	public Optional<E> replace(String name, E newEntry){
		Optional<E> replacedEntry = Optional.ofNullable(entryMap.replace(name, newEntry));
		replacedEntry.ifPresent(oldEntry -> eventProcessor.processEvent(new EntryReplacedEvent<>(oldEntry, newEntry)));
		return replacedEntry;
	}

	@Override
	public void addConfiguration(String configName, C configuration) {
		if (configName.equals(DEFAULT_CONFIG)) {
			throw new IllegalArgumentException("You cannot use 'default' as a configuration name as it is preserved for default configuration");
		}
		this.configurations.put(configName, configuration);
	}

	@Override
	public Optional<C> getConfiguration(String configName) {
		return Optional.ofNullable(this.configurations.get(configName));
	}

	@Override
	public C getDefaultConfig() {
		return configurations.get(DEFAULT_CONFIG);
	}

	@Override
	public EventPublisher<E> getEventPublisher() {
		return eventProcessor;
	}

	private class RegistryEventProcessor extends EventProcessor<RegistryEvent> implements EventConsumer<RegistryEvent>, EventPublisher<E> {

		@Override
		public EventPublisher<E> onEntryAdded(EventConsumer<EntryAddedEvent<E>> onSuccessEventConsumer) {
			registerConsumer(EntryAddedEvent.class.getSimpleName(), onSuccessEventConsumer);
			return this;
		}

		@Override
		public EventPublisher<E> onEntryRemoved(EventConsumer<EntryRemovedEvent<E>> onErrorEventConsumer) {
			registerConsumer(EntryRemovedEvent.class.getSimpleName(), onErrorEventConsumer);
			return this;
		}

		@Override
		public EventPublisher<E> onEntryReplaced(EventConsumer<EntryReplacedEvent<E>> onStateTransitionEventConsumer) {
			registerConsumer(EntryReplacedEvent.class.getSimpleName(), onStateTransitionEventConsumer);
			return this;
		}

		@Override
		public void consumeEvent(RegistryEvent event) {
			super.processEvent(event);
		}
	}

}
