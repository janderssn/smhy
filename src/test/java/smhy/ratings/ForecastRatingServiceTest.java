package smhy.ratings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smhy.conditions.ForecastCondition;
import smhy.conditions.WindDirection;
import smhy.forecasts.WeatherForecast;
import smhy.locations.ForecastLocation;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ForecastRatingServiceTest extends AbstractForecastRatingTest {

    @BeforeEach
    void setUp() {
        super.setUp();
    }

    @Test
    void rateForecastShouldPassWhenConditionDoesNotSpecifyDirection() {
        ForecastCondition condition = conditionBuilder.build();
        assertEquals(1.0, score(List.of(condition), weakWindFrom(N)));
    }

    @Test
    void rateForecastShouldPassWhenDegreeIsInsideBounds() {
        ForecastCondition condition = conditionBuilder
                .windDirectionMin(-1.0)
                .windDirectionMax(+1.0)
                .build();
        assertEquals(1.0, score(List.of(condition), weakWindFrom(N)));
    }

    @Test
    void rateForecastShouldPassWhenWindDirectionIsInsideBounds() {
        ForecastCondition condition = conditionBuilder
                .windDirections(List.of(WindDirection.N))
                .build();
        assertEquals(1.0, score(List.of(condition), weakWindFrom(N)));
    }

    @Test
    void rateForecastShouldPassWhenWindDirectionIsInsideBoundsMultiple() {
        ForecastCondition condition = conditionBuilder
                .windDirections(List.of(WindDirection.N, WindDirection.S))
                .build();
        assertEquals(1.0, score(List.of(condition), weakWindFrom(S)));
    }

    @Test
    void rateForecastShouldNotPassWhenWindDirectionIsOutsideBounds() {
        ForecastCondition condition = conditionBuilder
                .windDirections(List.of(WindDirection.S))
                .build();
        assertEquals(0.0, score(List.of(condition), weakWindFrom(N)));
    }

    @Test
    void rateForecastShouldNotPassWhenHourOfDayIsLessThanTimeOfDayMin() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder
                .timeOfDayMin(now.atZone(ZoneId.of("Europe/Stockholm")).plus(1, ChronoUnit.HOURS).get(ChronoField.HOUR_OF_DAY))
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        assertEquals(0.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldNotPassWhenHourOfDayIsGreaterThanTimeOfDayMax() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder
                .timeOfDayMax(now.atZone(ZoneId.of("Europe/Stockholm")).minus(1, ChronoUnit.HOURS).get(ChronoField.HOUR_OF_DAY))
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        assertEquals(0.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldPassWhenNoTimeOfDayMinOrMaxAreSet() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder.build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        assertEquals(1.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldNotPassWhenHourOfDayIsBeforeSunrise() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        weatherForecast.setSunrise(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).plus(1,ChronoUnit.HOURS)));
        weatherForecast.setSunset(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).plus(2,ChronoUnit.HOURS)));
        assertEquals(0.0, score(List.of(condition), weatherForecast, forecastItem));
    }

    @Test
    void rateForecastShouldNotPassWhenHourOfDayIsAfterSunset() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        weatherForecast.setSunrise(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).minus(2,ChronoUnit.HOURS)));
        weatherForecast.setSunset(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).minus(2,ChronoUnit.HOURS)));
        assertEquals(0.0, score(List.of(condition), weatherForecast, forecastItem));
    }

    @Test
    void rateForecastShouldPassWhenHourOfDayIsAfterSunriseAndBeforeSunset() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        weatherForecast.setSunrise(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).minus(1,ChronoUnit.HOURS)));
        weatherForecast.setSunset(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).plus(1,ChronoUnit.HOURS)));
        assertEquals(1.0, score(List.of(condition), weatherForecast, forecastItem));
    }

    @Test
    void rateForecastShouldNotPassBeforeSunriseEvenWhenTimeOfDayAllows() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder
                .timeOfDayMin(now.atZone(ZoneId.of("Europe/Stockholm")).minus(1,ChronoUnit.HOURS).get(ChronoField.HOUR_OF_DAY))
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        weatherForecast.setSunrise(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).plus(1,ChronoUnit.HOURS)));
        weatherForecast.setSunset(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).plus(1,ChronoUnit.HOURS)));
        assertEquals(0.0, score(List.of(condition), weatherForecast, forecastItem));
    }

    @Test
    void rateForecastShouldNotPassAfterSunsetEvenWhenTimeOfDayAllows() {
        Instant now = Instant.now(clock);
        ForecastCondition condition = conditionBuilder
                .timeOfDayMax(now.atZone(ZoneId.of("Europe/Stockholm")).plus(1,ChronoUnit.HOURS).get(ChronoField.HOUR_OF_DAY))
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .time(now)
                .build();
        weatherForecast.setSunrise(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).minus(1,ChronoUnit.HOURS)));
        weatherForecast.setSunset(LocalTime.from(now.atZone(ZoneId.of("Europe/Stockholm")).minus(1,ChronoUnit.HOURS)));
        assertEquals(0.0, score(List.of(condition), weatherForecast, forecastItem));
    }

    // --- Empty conditions ---

    @Test
    void rateForecastItemUsingConditions_emptyConditionsList_returnsEmptyOptional() {
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();
        Optional<ForecastRating.SiteRating> result =
                forecastRatingService.rateForecastItemUsingConditions(weatherForecast, forecastItem, Collections.emptyList());
        assertTrue(result.isEmpty(), "empty conditions should produce Optional.empty()");
    }

    // --- Calendar check ---

    @Test
    void rateForecastShouldScoreZeroWhenDayOfWeekNotInCondition() {
        // forecastBuilder uses 2022-07-01T13:00:00Z which is a Friday (day 5 in Stockholm local)
        ForecastCondition condition = conditionBuilder
                .daysOfWeek(List.of(6, 7)) // Saturday and Sunday only
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();
        assertEquals(0.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldScoreZeroWhenMonthOfYearNotInCondition() {
        // forecastBuilder uses 2022-07-01 which is July (month 7)
        ForecastCondition condition = conditionBuilder
                .monthsOfYear(List.of(1, 2, 3)) // Jan, Feb, Mar only
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();
        assertEquals(0.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldPassWhenDayOfWeekMatchesCondition() {
        // 2022-07-01 is a Friday, day 5
        ForecastCondition condition = conditionBuilder
                .daysOfWeek(List.of(5))
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();
        assertEquals(1.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldPassWhenMonthOfYearMatchesCondition() {
        // 2022-07-01 is July, month 7
        ForecastCondition condition = conditionBuilder
                .monthsOfYear(List.of(7))
                .build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();
        assertEquals(1.0, score(List.of(condition), forecastItem));
    }

    // --- Wind direction degree bounds check ---

    @Test
    void rateForecastShouldScoreZeroWhenWindDegreeIsOutsideBounds() {
        ForecastCondition condition = conditionBuilder
                .windDirectionMin(90.0)
                .windDirectionMax(270.0)
                .build();
        // North (0 degrees) is outside [90, 270]
        assertEquals(0.0, score(List.of(condition), weakWindFrom(N)));
    }

    // --- Wind speed check ---

    @Test
    void rateForecastShouldScoreZeroWhenWindSpeedBelowMin() {
        ForecastCondition condition = conditionBuilder
                .windSpeedMin(5.0)
                .windSpeedMax(15.0)
                .windGustMax(100.0)
                .build();
        // weakWindFrom uses speed 1.0, which is below min of 5.0
        assertEquals(0.0, score(List.of(condition), weakWindFrom(N)));
    }

    @Test
    void rateForecastShouldScoreZeroWhenWindSpeedAboveMax() {
        ForecastCondition condition = conditionBuilder
                .windSpeedMin(0.0)
                .windSpeedMax(3.0)
                .windGustMax(100.0)
                .build();
        // forecastStrongWindFrom uses speed 8.0, which is above max of 3.0
        assertEquals(0.0, score(List.of(condition), forecastStrongWindFrom(N)));
    }

    @Test
    void rateForecastShouldScoreZeroWhenWindGustsAboveMax() {
        ForecastCondition condition = conditionBuilder
                .windSpeedMin(0.0)
                .windSpeedMax(100.0)
                .windGustMax(5.0)
                .build();
        // forecastStrongWindFrom uses gusts 12.0, which is above max of 5.0
        assertEquals(0.0, score(List.of(condition), forecastStrongWindFrom(N)));
    }

    // --- Precipitation checks ---

    @Test
    void rateForecastShouldScoreZeroWhenPrecipitationAmountIsPositive() {
        ForecastCondition condition = conditionBuilder.build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .precipitationAmount(0.5)
                .precipitationAmountMax(0.0)
                .probabilityPrecipitation(0.0)
                .build();
        assertEquals(0.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldScoreZeroWhenPrecipitationAmountMaxIsPositive() {
        ForecastCondition condition = conditionBuilder.build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .precipitationAmount(0.0)
                .precipitationAmountMax(0.3)
                .probabilityPrecipitation(0.0)
                .build();
        assertEquals(0.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldScoreZeroWhenProbabilityPrecipitationAbove50() {
        ForecastCondition condition = conditionBuilder.build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .precipitationAmount(0.0)
                .precipitationAmountMax(0.0)
                .probabilityPrecipitation(51.0)
                .build();
        assertEquals(0.0, score(List.of(condition), forecastItem));
    }

    @Test
    void rateForecastShouldPassWhenProbabilityPrecipitationIsExactly50() {
        ForecastCondition condition = conditionBuilder.build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder
                .precipitationAmount(0.0)
                .precipitationAmountMax(0.0)
                .probabilityPrecipitation(50.0)
                .build();
        assertEquals(1.0, score(List.of(condition), forecastItem));
    }

    // --- max() tiebreaker ---

    @Test
    void rateForecastItemUsingConditions_higherScoringConditionWins() {
        ForecastCondition lowScore = conditionBuilder.score(0.5).build();
        ForecastCondition highScore = conditionBuilder.score(1.0).build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();

        Optional<ForecastRating.SiteRating> result =
                forecastRatingService.rateForecastItemUsingConditions(weatherForecast, forecastItem, List.of(lowScore, highScore));
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get().getScore());
    }

    @Test
    void rateForecastItemUsingConditions_equalScoreReturnsFirst() {
        ForecastCondition conditionA = conditionBuilder.score(1.0).location("locationA").build();
        ForecastCondition conditionB = conditionBuilder.score(1.0).location("locationB").build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();

        Optional<ForecastRating.SiteRating> result =
                forecastRatingService.rateForecastItemUsingConditions(weatherForecast, forecastItem, List.of(conditionA, conditionB));
        assertTrue(result.isPresent());
        // a.score >= b.score → a is returned; first condition (locationA) should win
        assertEquals("locationA", result.get().getLocation());
    }

    @Test
    void rateForecastItemUsingConditions_lowerScoredConditionLoses() {
        // Ensures b wins when b.score > a.score (covers the else branch of max())
        ForecastCondition conditionA = conditionBuilder.score(0.3).location("locationA").build();
        ForecastCondition conditionB = conditionBuilder.score(0.9).location("locationB").build();
        WeatherForecast.WeatherForecastItem forecastItem = forecastBuilder.build();

        Optional<ForecastRating.SiteRating> result =
                forecastRatingService.rateForecastItemUsingConditions(weatherForecast, forecastItem, List.of(conditionA, conditionB));
        assertTrue(result.isPresent());
        assertEquals("locationB", result.get().getLocation());
    }

    @Test
    void rateForecastUsesSiteSunriseWhenForecastHasNoSunTimes() {
        weatherForecast.setForecastLocation(ForecastLocation.builder()
                .id("granholmstoppen")
                .latitude("59.4")
                .longitude("17.86667")
                .build());
        weatherForecast.setSunrise(null);
        weatherForecast.setSunset(null);
        ForecastCondition condition = conditionBuilder
                .timeOfDayMin(0)
                .timeOfDayMax(23)
                .build();
        ZoneId stockholm = ZoneId.of("Europe/Stockholm");
        Instant beforeSunrise = ZonedDateTime.of(2022, 7, 1, 2, 0, 0, 0, stockholm).toInstant();
        Instant noon = ZonedDateTime.of(2022, 7, 1, 12, 0, 0, 0, stockholm).toInstant();

        assertEquals(0.0, score(List.of(condition), forecastBuilder.time(beforeSunrise).build()));
        assertEquals(1.0, score(List.of(condition), forecastBuilder.time(noon).build()));
    }
}