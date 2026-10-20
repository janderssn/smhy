package smhy.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smhy.conditions.ForecastConditionService;
import smhy.conditions.ForecastConditionYamlFileStore;
import smhy.forecasts.InMemoryForecastStore;
import smhy.forecasts.WeatherForecast;
import smhy.forecasts.WeatherForecastInterpolator;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;
import smhy.locations.ForecastLocationYamlFileStore;
import smhy.ratings.CalendarChecker;
import smhy.ratings.DegreeToDirectionConverter;
import smhy.ratings.DirectionBoundsChecker;
import smhy.ratings.ForecastRatingAggregator;
import smhy.ratings.ForecastRatingService;
import smhy.ratings.TimeOfDayChecker;
import smhy.ratings.WindChecker;
import smhy.sun.SunCalculator;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceTest {

    private DashboardService dashboardService;
    private ForecastLocation granholm;

    @BeforeEach
    void setUp() {
        ForecastLocationService locations = new ForecastLocationService(new ForecastLocationYamlFileStore());
        locations.load(Path.of("src/main/resources/config/locations.yml"));
        ForecastConditionService conditions = new ForecastConditionService(new ForecastConditionYamlFileStore());
        conditions.load(Path.of("src/main/resources/config/conditions.yml"));
        InMemoryForecastStore store = new InMemoryForecastStore();
        ForecastRatingService ratings = new ForecastRatingService(
                conditions,
                locations,
                new ForecastRatingAggregator(),
                new WeatherForecastInterpolator(),
                new WindChecker(new DegreeToDirectionConverter(), new DirectionBoundsChecker()),
                new TimeOfDayChecker(),
                new CalendarChecker());
        dashboardService = new DashboardService(
                locations,
                store,
                ratings,
                new SunCalculator(),
                new LaunchRules(conditions),
                new ForecastSourceCatalog("yr"));
        granholm = locations.findById("granholmstoppen").orElseThrow();
        store.save(forecast(granholm, LocalDate.of(2026, 10, 20)));
    }

    @Test
    void granholmWindowIsTrimmedToDaylightAndUsesRealNumbers() {
        LocalDate day = LocalDate.of(2026, 10, 20);
        SunCalculator.Span sun = new SunCalculator().on(59.4, 17.86667, day).orElseThrow();
        int from = -1;
        int to = -1;
        for (int hour = 4; hour <= 19; hour++) {
            LocalTime time = LocalTime.of(hour, 0);
            boolean daylight = !time.isBefore(sun.rise()) && time.isBefore(sun.set());
            if (daylight && hour >= 6 && hour <= 20) {
                if (from < 0) {
                    from = hour;
                }
                to = hour;
            }
        }

        Dashboard dashboard = dashboardService.build(day);
        Dashboard.Site site = site(dashboard, "granholmstoppen");
        Dashboard.Window window = site.getWindows().stream()
                .filter(item -> day.toString().equals(item.getDate()))
                .findFirst()
                .orElseThrow();

        assertEquals(from, window.getFromHour());
        assertEquals(to, window.getToHour());
        assertTrue(from >= 7, "sunrise should trim the 06:00 clock start");
        assertTrue(to <= 18, "sunset should trim the evening");
        assertEquals(10, window.getScore());
        assertEquals(3.0, window.getWind());
        assertEquals(5.0, window.getGust());
        assertEquals(8.0, window.getTemp());
        assertEquals(6.0, window.getDew());
        assertEquals(250, window.getCloudBase());
        assertTrue(window.isCloudBaseCalculated());
        assertEquals(20.0, window.getDirection());

        Dashboard.Hour before = hour(site, from - 1);
        Dashboard.Hour inside = hour(site, from);
        assertNull(before.getScore());
        assertFalse(before.isDaylight());
        assertEquals(10, inside.getScore());
        assertTrue(inside.isDaylight());
    }

    @Test
    void comingSourcesHaveNoLiveStatusAndFavoritesAreTheThreeHills() {
        Dashboard dashboard = dashboardService.build(LocalDate.of(2026, 10, 20));

        assertEquals(1, dashboard.getSchema());
        assertEquals("yr", dashboard.getActiveSource());
        assertEquals("se/stockholm", dashboard.getRegionView());
        assertEquals("Stockholm", dashboard.getRegionName());
        assertEquals(List.of("areskutan", "alleberg", "kaseberga"), dashboard.getDefaultFavorites());
        assertEquals("live", source(dashboard, "yr").getStatus());
        assertEquals("coming", source(dashboard, "smhi").getStatus());
        assertEquals("coming", source(dashboard, "open-meteo").getStatus());
        assertEquals("coming", source(dashboard, "holfuy").getStatus());
        assertEquals(3, dashboard.getDays().get(0).getConfidence());
        assertEquals("Hög säkerhet", dashboard.getDays().get(0).getConfidenceLabel());
        assertEquals("lead-time", dashboard.getDays().get(0).getConfidenceBasis());

        Dashboard.Launch areskutan = site(dashboard, "areskutan").getLaunch();
        assertTrue(areskutan.getGood().containsAll(List.of("SSW", "S", "SSE", "SE")));
        assertFalse(areskutan.getGood().contains("NW"));
        assertNull(site(dashboard, "granholmstoppen").getHolfuy());
        assertEquals(302, site(dashboard, "areskutan").getHolfuy());
        assertNull(site(dashboard, "salanda").getHolfuy());
        assertEquals("Jönköping", site(dashboard, "alleberg").getRegionName());
        assertTrue(site(dashboard, "alleberg").getWindows().isEmpty());
        assertTrue(dashboard.getNotes().stream().anyMatch(note -> note.contains("SMHI")));
    }

    private static Dashboard.Site site(Dashboard dashboard, String id) {
        return dashboard.getSites().stream().filter(site -> id.equals(site.getId())).findFirst().orElseThrow();
    }

    private static Dashboard.Hour hour(Dashboard.Site site, int hour) {
        return site.getHours().stream().filter(item -> item.getHour() == hour).findFirst().orElseThrow();
    }

    private static Dashboard.Source source(Dashboard dashboard, String id) {
        return dashboard.getSources().stream().filter(source -> id.equals(source.getId())).findFirst().orElseThrow();
    }

    private static WeatherForecast forecast(ForecastLocation location, LocalDate date) {
        List<WeatherForecast.WeatherForecastItem> items = new ArrayList<>();
        for (int hour = 4; hour <= 19; hour++) {
            items.add(WeatherForecast.WeatherForecastItem.builder()
                    .time(date.atTime(hour, 0).atZone(SunCalculator.ZONE).toInstant())
                    .windSpeed(3.0)
                    .windGusts(5.0)
                    .windDirection(20.0)
                    .airTemperature(8.0)
                    .dewPointTemperature(6.0)
                    .airPressure(1030.0)
                    .relativeHumidity(90.0)
                    .precipitationAmount(0.0)
                    .precipitationAmountMin(0.0)
                    .precipitationAmountMax(0.0)
                    .probabilityPrecipitation(0.0)
                    .probabilityThunder(0.0)
                    .build());
        }
        return WeatherForecast.builder()
                .provider("yr")
                .forecastLocation(location)
                .forecastDate(Instant.parse("2026-10-20T06:00:00Z"))
                .items(items)
                .build();
    }
}
