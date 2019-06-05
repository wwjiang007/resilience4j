package io.github.resilience4j.core.registry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.resilience4j.core.registry.RegistryEvent.Type;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AbstractRegistryTest {

	@Test
	public void shouldContainDefaultAndCustomConfiguration() {
		TestRegistry testRegistry = new TestRegistry();
		testRegistry.addConfiguration("custom", "test");
		assertThat(testRegistry.getConfiguration("custom").get()).isEqualTo("test");
		assertThat(testRegistry.getDefaultConfig()).isEqualTo("default");
	}

	@Test
	public void shouldNotAllowToOverwriteDefaultConfiguration() {
		TestRegistry testRegistry = new TestRegistry();

		assertThatThrownBy(() ->testRegistry.addConfiguration("default", "test") )
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldConsumeRegistryEvents() {
		List<RegistryEvent> consumedEvents = new ArrayList<>();
		List<EntryAddedEvent<String>> addedEvents = new ArrayList<>();
		List<EntryRemovedEvent<String>> removedEvents = new ArrayList<>();
		List<EntryReplacedEvent<String>> replacedEvents = new ArrayList<>();

		TestRegistry testRegistry = new TestRegistry();
		testRegistry.getEventPublisher().onEvent(consumedEvents::add);
		testRegistry.getEventPublisher().onEntryAdded(addedEvents::add);
		testRegistry.getEventPublisher().onEntryRemoved(removedEvents::add);
		testRegistry.getEventPublisher().onEntryReplaced(replacedEvents::add);

		String addedEntry1 = testRegistry.computeIfAbsent("name", () -> "entry1");
		assertThat(addedEntry1).isEqualTo("entry1");

		String addedEntry2 = testRegistry.computeIfAbsent("name2", () -> "entry2");
		assertThat(addedEntry2).isEqualTo("entry2");

		Optional<String> removedEntry = testRegistry.remove("name");
		assertThat(removedEntry).isNotEmpty().hasValue("entry1");

		Optional<String> replacedEntry = testRegistry.replace("name2", "entry3");
		assertThat(replacedEntry).isNotEmpty().hasValue("entry2");

		assertThat(consumedEvents).hasSize(4);
		assertThat(addedEvents).hasSize(2);
		assertThat(removedEvents).hasSize(1);
		assertThat(replacedEvents).hasSize(1);

		assertThat(consumedEvents).extracting("eventType")
				.containsExactly(Type.ADDED, Type.ADDED, Type.REMOVED, Type.REPLACED);

		assertThat(addedEvents).extracting("addedEntry")
				.containsExactly("entry1", "entry2");

		assertThat(removedEvents).extracting("removedEntry")
				.containsExactly("entry1");

		assertThat(replacedEvents).extracting("oldEntry")
				.containsExactly("entry2");

		assertThat(replacedEvents).extracting("newEntry")
				.containsExactly("entry3");

	}

	@Test
	public void shouldOnlyFindRegisteredObjects() {
		TestRegistry testRegistry = new TestRegistry();

		assertThat(testRegistry.find("test")).isEmpty();
		testRegistry.entryMap.put("test", "value");
		assertThat(testRegistry.find("test")).contains("value");
	}


	class TestRegistry extends AbstractRegistry<String, String> {

		public TestRegistry() {
			super( "default");
			this.configurations.put(DEFAULT_CONFIG, "default");

		}
	}

}
