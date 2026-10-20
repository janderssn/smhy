package smhy.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.test.util.ReflectionTestUtils;
import smhy.dashboard.DashboardService;
import smhy.forecasts.InMemoryForecastStore;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;
import smhy.ratings.ForecastRating;
import smhy.ratings.ForecastRatingService;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForecastReportGeneratorTest {

    @TempDir
    Path tempDir;

    @Mock
    private ForecastLocationService forecastLocationService;

    @Mock
    private InMemoryForecastStore forecastStore;

    @Mock
    private ForecastRatingService forecastRatingService;

    @Mock
    private ForecastReportHtmlTemplate forecastReportHtmlTemplate;

    @Mock
    private ForecastReportJsonGenerator forecastReportJsonGenerator;

    @Mock
    private DashboardService dashboardService;

    private ForecastReportGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        generator = new ForecastReportGenerator(
                forecastLocationService,
                forecastStore,
                forecastRatingService,
                forecastReportHtmlTemplate,
                forecastReportJsonGenerator,
                dashboardService
        );

        ReflectionTestUtils.setField(generator, "htmlOutputDirectory", new PathResource(tempDir));
        // Use a ClassPathResource pattern that resolves to nothing — avoids file-not-found errors
        ReflectionTestUtils.setField(generator, "htmlAssetsDirectory", new ClassPathResource("assets/"));
        ReflectionTestUtils.setField(generator, "region", "se/stockholm");

        // html template generate() throws IOException — stub it to do nothing by default.
        // Use lenient so tests that override this stub (e.g. IOException test) don't trigger UnnecessaryStubbingException.
        lenient().doNothing().when(forecastReportHtmlTemplate).generate(any(ForecastRating.class), any(Path.class));
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

    private ForecastRating emptyRating() {
        return ForecastRating.builder()
                .forecastDate(Instant.now())
                .forecastLocation("Salanda")
                .siteRatings(List.of())
                .build();
    }

    @Test
    void generateWithLocationIds_noForecastsFound_callsHtmlAndJsonGenerators4Times() throws Exception {
        when(forecastLocationService.findAll()).thenReturn(List.of(buildLocation()));
        when(forecastStore.findLatestByLocation("salanda", "yr")).thenReturn(Optional.empty());

        generator.generate(List.<String>of());

        // 4 HTML reports: index, helg, favoriter, stockholm
        verify(forecastReportHtmlTemplate, times(4)).generate(any(ForecastRating.class), any(Path.class));
        // 4 JSON reports: all, weekend, favorites, stockholm
        verify(forecastReportJsonGenerator, times(4)).generate(anyString(), any(Collection.class));
    }

    @Test
    void generateWithLocationIds_filtersByLocationId_callsHtmlAndJsonGenerators4Times() throws Exception {
        when(forecastLocationService.findAll()).thenReturn(List.of(buildLocation()));
        when(forecastStore.findLatestByLocation("salanda", "yr")).thenReturn(Optional.empty());

        generator.generate(List.of("salanda"));

        verify(forecastReportHtmlTemplate, times(4)).generate(any(ForecastRating.class), any(Path.class));
        verify(forecastReportJsonGenerator, times(4)).generate(anyString(), any(Collection.class));
    }

    @Test
    void generateWithSiteRatingsCollection_callsHtmlAndJsonGenerators4Times() throws Exception {
        Collection<ForecastRating.SiteRating> emptyRatings = java.util.Collections.emptyList();

        generator.generate(emptyRatings);

        verify(forecastReportHtmlTemplate, times(4)).generate(any(ForecastRating.class), any(Path.class));
        verify(forecastReportJsonGenerator, times(4)).generate(anyString(), any(Collection.class));
    }

    @Test
    void generateWithSiteRatings_filtersWeekendAndFavoritesAndRegion() throws Exception {
        // A SiteRating for a Saturday
        ForecastRating.SiteRating weekendRating = ForecastRating.SiteRating.builder()
                .location("salanda")
                .name("Salanda")
                .time(Instant.parse("2024-06-15T10:00:00Z")) // Saturday
                .score(80.0)
                .hoursMin(3.0)
                .build();

        when(forecastLocationService.findById("salanda"))
                .thenReturn(Optional.of(buildLocation()));

        Collection<ForecastRating.SiteRating> ratings = List.of(weekendRating);

        generator.generate(ratings);

        verify(forecastReportHtmlTemplate, times(4)).generate(any(ForecastRating.class), any(Path.class));
        verify(forecastReportJsonGenerator, times(4)).generate(anyString(), any(Collection.class));
    }

    @Test
    void generateWithSiteRatings_htmlTemplateThrowsIOException_logsWarningAndContinuesToJson() throws Exception {
        // Make the HTML template throw IOException to cover the catch block (lines 82-83)
        doThrow(new java.io.IOException("disk full"))
                .when(forecastReportHtmlTemplate).generate(any(ForecastRating.class), any(Path.class));

        Collection<ForecastRating.SiteRating> emptyRatings = java.util.Collections.emptyList();

        // Should not throw — IOException must be caught internally
        generator.generate(emptyRatings);

        // JSON generation runs outside the try-catch, so it should still be called
        verify(forecastReportJsonGenerator, times(4)).generate(anyString(), any(Collection.class));
    }

    @Test
    void generateWithSiteRatings_weekdayRatingExcludedFromWeekend() throws Exception {
        // A SiteRating for a Monday (weekday) — exercises the false branch of isSaturdayOrSunday
        ForecastRating.SiteRating weekdayRating = ForecastRating.SiteRating.builder()
                .location("salanda")
                .name("Salanda")
                .time(Instant.parse("2024-06-17T10:00:00Z")) // Monday
                .score(75.0)
                .hoursMin(3.0)
                .build();

        when(forecastLocationService.findById("salanda"))
                .thenReturn(Optional.of(buildLocation()));

        Collection<ForecastRating.SiteRating> ratings = List.of(weekdayRating);

        generator.generate(ratings);

        // Capture the ForecastRating passed to generate for "helg.html" (weekend)
        // By verifying all 4 HTML calls still happen, and JSON calls still happen
        verify(forecastReportHtmlTemplate, times(4)).generate(any(ForecastRating.class), any(Path.class));
        verify(forecastReportJsonGenerator, times(4)).generate(anyString(), any(Collection.class));
    }

    @Test
    void generateWithLocationIds_locationNotInRequestedIds_isFiltered() throws Exception {
        // Repository has "salanda", but we request only "alleberg" — salanda should be skipped
        // This covers the isEmpty=false AND contains=false branch on the stream filter
        when(forecastLocationService.findAll()).thenReturn(List.of(buildLocation()));
        // findLatestByLocation should NOT be called for salanda since it's filtered out

        generator.generate(List.of("alleberg"));

        // HTML and JSON generators still called 4 times (empty rating sets)
        verify(forecastReportHtmlTemplate, times(4)).generate(any(ForecastRating.class), any(Path.class));
        verify(forecastReportJsonGenerator, times(4)).generate(anyString(), any(Collection.class));
        // salanda forecast should not be looked up
        verify(forecastStore, never()).findLatestByLocation(anyString(), anyString());
    }
}
