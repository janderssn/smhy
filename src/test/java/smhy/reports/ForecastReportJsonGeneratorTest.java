package smhy.reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.PathResource;
import org.springframework.test.util.ReflectionTestUtils;
import smhy.JsonExportConfiguration;
import smhy.forecasts.InMemoryForecastStore;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;
import smhy.ratings.ForecastRating;

import smhy.forecasts.WeatherForecast;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastReportJsonGeneratorTest {

    @TempDir
    Path tempDir;

    @Mock
    private ForecastLocationService forecastLocationService;

    @Mock
    private InMemoryForecastStore forecastStore;

    @Mock
    private JsonExportConfiguration jsonExportConfiguration;

    private ForecastReportJsonGenerator generator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        generator = new ForecastReportJsonGenerator(
                forecastLocationService,
                forecastStore,
                objectMapper,
                jsonExportConfiguration
        );

        ReflectionTestUtils.setField(generator, "jsonOutputDirectory", new PathResource(tempDir));
    }

    private ForecastRating.SiteRating buildSiteRating() {
        return ForecastRating.SiteRating.builder()
                .location("salanda")
                .name("Salanda")
                .time(Instant.parse("2024-06-15T10:00:00Z"))
                .score(80.0)
                .windDirectionMin(180.0)
                .windDirectionMax(270.0)
                .windSpeedMin(3.0)
                .windSpeedMax(8.0)
                .windGustMax(12.0)
                .airTemperatureMin(10.0)
                .airTemperatureMax(25.0)
                .relativeHumidityMin(0.0)
                .relativeHumidityMax(80.0)
                .airPressureMin(1000.0)
                .airPressureMax(1030.0)
                .timeOfDayMin(8)
                .timeOfDayMax(18)
                .hoursMin(3.0)
                .probabilityPrecipitationMin(0.0)
                .probabilityPrecipitationMax(20.0)
                .precipitationAmountMin(0.0)
                .precipitationAmountMax(1.0)
                .dewPointTemperatureMin(0.0)
                .dewPointTemperatureMax(15.0)
                .build();
    }

    private ForecastLocation buildLocation() {
        return ForecastLocation.builder()
                .id("salanda")
                .name("Salanda")
                .region("se/stockholm")
                .latitude("59.0")
                .longitude("17.0")
                .altitude("100")
                .favorite(true)
                .takeoffs(List.of("Salanda N"))
                .build();
    }

    @Test
    void generate_whenEnabled_createsJsonFile() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(true);
        when(forecastLocationService.findById("salanda")).thenReturn(Optional.of(buildLocation()));
        when(forecastStore.findLatestByLocation("salanda", "yr")).thenReturn(Optional.empty());

        List<ForecastRating.SiteRating> ratings = List.of(buildSiteRating());

        generator.generate("all", ratings);

        Path expectedFile = tempDir.resolve("all.json");
        assertTrue(expectedFile.toFile().exists(), "Expected all.json to be created");
        assertTrue(expectedFile.toFile().length() > 0, "Expected all.json to have content");
    }

    @Test
    void generate_whenDisabled_doesNotCreateFile() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(false);

        List<ForecastRating.SiteRating> ratings = List.of(buildSiteRating());

        generator.generate("all", ratings);

        Path expectedFile = tempDir.resolve("all.json");
        assertFalse(expectedFile.toFile().exists(), "Expected all.json NOT to be created when disabled");
    }

    @Test
    void generate_withEmptyRatings_createsJsonFileWithEmptyLocations() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(true);

        generator.generate("weekend", List.of());

        Path expectedFile = tempDir.resolve("weekend.json");
        assertTrue(expectedFile.toFile().exists(), "Expected weekend.json to be created");
    }

    @Test
    void generate_withRealForecast_includesForecastDataInJson() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(true);
        when(forecastLocationService.findById("salanda")).thenReturn(Optional.of(buildLocation()));

        WeatherForecast.WeatherForecastItem item = WeatherForecast.WeatherForecastItem.builder()
                .time(Instant.parse("2024-06-15T10:00:00Z"))
                .windSpeed(5.0)
                .windDirection(220.0)
                .windGusts(9.0)
                .airPressure(1013.0)
                .airTemperature(18.0)
                .dewPointTemperature(10.0)
                .relativeHumidity(60.0)
                .precipitationAmount(0.1)
                .precipitationAmountMin(0.0)
                .precipitationAmountMax(0.5)
                .probabilityPrecipitation(15.0)
                .probabilityThunder(2.0)
                .build();

        WeatherForecast forecast = WeatherForecast.builder()
                .id("forecast-1")
                .provider("yr")
                .forecastDate(Instant.parse("2024-06-15T06:00:00Z"))
                .sunrise(LocalTime.of(4, 30))
                .sunset(LocalTime.of(21, 45))
                .items(List.of(item))
                .build();

        when(forecastStore.findLatestByLocation("salanda", "yr")).thenReturn(Optional.of(forecast));

        generator.generate("all", List.of(buildSiteRating()));

        Path outputFile = tempDir.resolve("all.json");
        assertTrue(outputFile.toFile().exists());
        String json = Files.readString(outputFile, StandardCharsets.UTF_8);

        // Verify forecast data is present
        assertTrue(json.contains("\"provider\" : \"yr\""), "Expected provider 'yr' in JSON");
        assertTrue(json.contains("\"windSpeed\" : 5.0"), "Expected windSpeed in JSON");
        assertTrue(json.contains("\"windDirection\" : 220.0"), "Expected windDirection in JSON");
        assertTrue(json.contains("\"airTemperature\" : 18.0"), "Expected airTemperature in JSON");
        assertTrue(json.contains("\"dewPointTemperature\" : 10.0"), "Expected dewPointTemperature in JSON");
        assertTrue(json.contains("\"relativeHumidity\" : 60.0"), "Expected relativeHumidity in JSON");
        assertTrue(json.contains("\"probabilityThunder\" : 2.0"), "Expected probabilityThunder in JSON");
    }

    @Test
    void generate_withMultipleRatingsForSameLocation_includesAllRatingsInJson() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(true);
        when(forecastLocationService.findById("salanda")).thenReturn(Optional.of(buildLocation()));
        when(forecastStore.findLatestByLocation("salanda", "yr")).thenReturn(Optional.empty());

        ForecastRating.SiteRating rating1 = buildSiteRating();
        ForecastRating.SiteRating rating2 = ForecastRating.SiteRating.builder()
                .location("salanda")
                .name("Salanda")
                .time(Instant.parse("2024-06-15T14:00:00Z"))
                .score(60.0)
                .windDirectionMin(160.0)
                .windDirectionMax(250.0)
                .windSpeedMin(2.0)
                .windSpeedMax(6.0)
                .windGustMax(10.0)
                .timeOfDayMin(10)
                .timeOfDayMax(16)
                .hoursMin(2.0)
                .build();

        generator.generate("all", List.of(rating1, rating2));

        Path outputFile = tempDir.resolve("all.json");
        String json = Files.readString(outputFile, StandardCharsets.UTF_8);

        // Both ratings should appear in the ratings array
        assertTrue(json.contains("\"score\" : 80.0"), "Expected score 80 in JSON");
        assertTrue(json.contains("\"score\" : 60.0"), "Expected score 60 in JSON");
        // Condition data should be mapped
        assertTrue(json.contains("\"windDirectionMin\" : 180.0"), "Expected windDirectionMin from rating1");
        assertTrue(json.contains("\"windDirectionMin\" : 160.0"), "Expected windDirectionMin from rating2");
        assertTrue(json.contains("\"timeOfDayMin\" : 8"), "Expected timeOfDayMin from rating1");
        assertTrue(json.contains("\"timeOfDayMin\" : 10"), "Expected timeOfDayMin from rating2");
    }

    @Test
    void generate_withReportTypeContainingSpaces_normalisesFilename() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(true);

        generator.generate("My Report", List.of());

        Path expectedFile = tempDir.resolve("my-report.json");
        assertTrue(expectedFile.toFile().exists(), "Expected my-report.json (spaces replaced by dashes, lowercased)");
        assertFalse(tempDir.resolve("My Report.json").toFile().exists(), "Expected no file with spaces in name");
    }

    @Test
    void generate_whenLocationNotFoundInRepository_skipsLocationInOutput() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(true);
        // Location "salanda" is NOT found
        when(forecastLocationService.findById("salanda")).thenReturn(Optional.empty());

        generator.generate("all", List.of(buildSiteRating()));

        Path outputFile = tempDir.resolve("all.json");
        assertTrue(outputFile.toFile().exists());
        String json = Files.readString(outputFile, StandardCharsets.UTF_8);

        // The locations array should be empty since the location was not found
        assertTrue(json.contains("\"locations\" : [ ]"), "Expected empty locations array when location not found");
    }

    @Test
    void generate_whenOutputDirectoryIsReadOnly_logsErrorAndDoesNotThrow() throws Exception {
        when(jsonExportConfiguration.isEnabled()).thenReturn(true);

        // Point output to a path whose parent does not exist and cannot be created (e.g. under /proc)
        // This causes Files.createDirectories or mapper.writeValue to throw an IOException,
        // covering the catch block (lines 61-62)
        Path nonWritablePath = Path.of("/proc/nonexistent-smhy-test-dir");
        ReflectionTestUtils.setField(generator, "jsonOutputDirectory", new PathResource(nonWritablePath));

        // Should not throw — the exception must be caught and logged internally
        generator.generate("all", List.of(buildSiteRating()));
    }
}
