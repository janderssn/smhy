package smhy.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import smhy.conditions.ForecastCondition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractYamlFileStoreTest {

    // Concrete subclass for testing – uses ForecastCondition as the type parameter
    static class TestYamlFileStore extends AbstractYamlFileStore<ForecastCondition> {
    }

    private static final String SAMPLE_YAML = """
            - id: test1
              location: salanda
              score: 80.0
              windSpeedMin: 3.0
              windSpeedMax: 10.0
            """;

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // load(Path)
    // -----------------------------------------------------------------------

    @Test
    void loadPathReturnsConditionsFromYamlFile() throws IOException {
        Path file = writeTempYaml(SAMPLE_YAML);
        TestYamlFileStore store = new TestYamlFileStore();

        Collection<ForecastCondition> result = store.load(file);

        assertEquals(1, result.size());
        ForecastCondition condition = result.iterator().next();
        assertEquals("test1", condition.getId());
        assertEquals("salanda", condition.getLocation());
        assertEquals(80.0, condition.getScore());
        assertEquals(3.0, condition.getWindSpeedMin());
        assertEquals(10.0, condition.getWindSpeedMax());
    }

    @Test
    void loadPathReturnsEmptyListForNonExistentFile() {
        Path missing = tempDir.resolve("does-not-exist.yml");
        TestYamlFileStore store = new TestYamlFileStore();

        Collection<ForecastCondition> result = store.load(missing);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // load(Resource)
    // -----------------------------------------------------------------------

    @Test
    void loadResourceReturnsConditionsFromYamlFile() throws IOException {
        Path file = writeTempYaml(SAMPLE_YAML);
        TestYamlFileStore store = new TestYamlFileStore();

        Collection<ForecastCondition> result = store.load(new FileSystemResource(file));

        assertEquals(1, result.size());
        ForecastCondition condition = result.iterator().next();
        assertEquals("test1", condition.getId());
        assertEquals("salanda", condition.getLocation());
        assertEquals(80.0, condition.getScore());
    }

    @Test
    void loadResourceReturnsEmptyListForNonExistentResource() {
        TestYamlFileStore store = new TestYamlFileStore();
        // FileSystemResource wrapping a non-existent path will fail to open stream
        FileSystemResource missing = new FileSystemResource(tempDir.resolve("missing.yml").toFile());

        Collection<ForecastCondition> result = store.load(missing);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // save(Path, Collection<T>)
    // -----------------------------------------------------------------------

    @Test
    void saveAndReloadConditions() throws IOException {
        TestYamlFileStore store = new TestYamlFileStore();
        ForecastCondition condition = ForecastCondition.builder()
                .id("save1")
                .location("alleberg")
                .score(70.0)
                .windSpeedMin(2.0)
                .windSpeedMax(8.0)
                .build();
        Path outputFile = tempDir.resolve("saved-conditions.yml");

        store.save(outputFile, List.of(condition));

        // Reload and verify
        Collection<ForecastCondition> loaded = store.load(outputFile);
        assertEquals(1, loaded.size());
        ForecastCondition reloaded = loaded.iterator().next();
        assertEquals("alleberg", reloaded.getLocation());
        assertEquals(70.0, reloaded.getScore());
        assertEquals(2.0, reloaded.getWindSpeedMin());
        assertEquals(8.0, reloaded.getWindSpeedMax());
    }

    @Test
    void saveEmptyCollection_returnsImmediatelyWithoutCreatingFile() {
        TestYamlFileStore store = new TestYamlFileStore();
        Path outputFile = tempDir.resolve("empty-conditions.yml");

        store.save(outputFile, List.of());

        // File should not exist because save() returns early for empty collection
        assertFalse(Files.exists(outputFile));
    }

    @Test
    void saveMultipleConditions_allPersistedAndReloadable() throws IOException {
        TestYamlFileStore store = new TestYamlFileStore();
        List<ForecastCondition> conditions = List.of(
                ForecastCondition.builder().id("c1").location("salanda").score(80.0).build(),
                ForecastCondition.builder().id("c2").location("alleberg").score(60.0).build()
        );
        Path outputFile = tempDir.resolve("multi-conditions.yml");

        store.save(outputFile, conditions);

        Collection<ForecastCondition> loaded = store.load(outputFile);
        assertEquals(2, loaded.size());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private Path writeTempYaml(String content) throws IOException {
        Path file = tempDir.resolve("conditions.yml");
        Files.writeString(file, content);
        return file;
    }
}
