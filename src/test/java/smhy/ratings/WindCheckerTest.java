package smhy.ratings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smhy.conditions.ForecastCondition;
import smhy.conditions.WindDirection;
import smhy.forecasts.WeatherForecast;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindCheckerTest {

    private WindChecker windChecker;

    private WeatherForecast.WeatherForecastItem item(double windDirection, double windSpeed, double windGusts) {
        return WeatherForecast.WeatherForecastItem.builder()
                .time(Instant.parse("2022-07-01T12:00:00Z"))
                .windDirection(windDirection)
                .windSpeed(windSpeed)
                .windGusts(windGusts)
                .build();
    }

    private ForecastCondition condition(Double speedMin, Double speedMax, Double gustMax) {
        return ForecastCondition.builder()
                .windSpeedMin(speedMin)
                .windSpeedMax(speedMax)
                .windGustMax(gustMax)
                .build();
    }

    @BeforeEach
    void setUp() {
        windChecker = new WindChecker(new DegreeToDirectionConverter(), new DirectionBoundsChecker());
    }

    // --- isWindSpeedWithinLimits ---

    @Test
    void isWindSpeedWithinLimits_passesWhenSpeedInRange() {
        assertTrue(windChecker.isWindSpeedWithinLimits(item(0, 5.0, 8.0), condition(3.0, 10.0, 15.0)));
    }

    @Test
    void isWindSpeedWithinLimits_failsWhenSpeedBelowMin() {
        // wind speed 2.0 is below min 5.0
        assertFalse(windChecker.isWindSpeedWithinLimits(item(0, 2.0, 3.0), condition(5.0, 10.0, 15.0)));
    }

    @Test
    void isWindSpeedWithinLimits_failsWhenSpeedAboveMax() {
        // wind speed 12.0 is above max 10.0
        assertFalse(windChecker.isWindSpeedWithinLimits(item(0, 12.0, 5.0), condition(0.0, 10.0, 15.0)));
    }

    @Test
    void isWindSpeedWithinLimits_failsWhenGustsAboveMax() {
        // gusts 20.0 are above gustMax 15.0; speed is within range
        assertFalse(windChecker.isWindSpeedWithinLimits(item(0, 5.0, 20.0), condition(0.0, 10.0, 15.0)));
    }

    @Test
    void isWindSpeedWithinLimits_passesWhenGustsAreMissing() {
        WeatherForecast.WeatherForecastItem missingGust = WeatherForecast.WeatherForecastItem.builder()
                .time(Instant.parse("2022-07-01T12:00:00Z"))
                .windDirection(0.0)
                .windSpeed(5.0)
                .build();
        assertTrue(windChecker.isWindSpeedWithinLimits(missingGust, condition(0.0, 10.0, 7.0)));
    }

    @Test
    void isWindSpeedWithinLimits_passesAtExactBoundaries() {
        // speed exactly at min and max, gusts exactly at gustMax
        assertTrue(windChecker.isWindSpeedWithinLimits(item(0, 5.0, 15.0), condition(5.0, 5.0, 15.0)));
    }

    // --- isWindDirectionWithinLimits: windDirections list path ---

    @Test
    void isWindDirectionWithinLimits_passesWhenDirectionInList() {
        ForecastCondition cond = ForecastCondition.builder()
                .windDirections(List.of(WindDirection.N, WindDirection.S))
                .build();
        assertTrue(windChecker.isWindDirectionWithinLimits(item(0.0, 1.0, 1.0), cond));
    }

    @Test
    void isWindDirectionWithinLimits_failsWhenDirectionNotInList() {
        ForecastCondition cond = ForecastCondition.builder()
                .windDirections(List.of(WindDirection.S))
                .build();
        // 0 degrees = N, which is not S
        assertFalse(windChecker.isWindDirectionWithinLimits(item(0.0, 1.0, 1.0), cond));
    }

    @Test
    void isWindDirectionWithinLimits_passesWhenNoBoundsAndNoList() {
        ForecastCondition cond = ForecastCondition.builder().build();
        assertTrue(windChecker.isWindDirectionWithinLimits(item(180.0, 1.0, 1.0), cond));
    }

    // --- isWindDirectionWithinLimits: degree bounds path ---

    @Test
    void isWindDirectionWithinLimits_passesWhenDegreeWithinBounds() {
        ForecastCondition cond = ForecastCondition.builder()
                .windDirectionMin(-45.0)
                .windDirectionMax(45.0)
                .build();
        assertTrue(windChecker.isWindDirectionWithinLimits(item(0.0, 1.0, 1.0), cond));
    }

    @Test
    void isWindDirectionWithinLimits_failsWhenDegreeOutsideBounds() {
        ForecastCondition cond = ForecastCondition.builder()
                .windDirectionMin(90.0)
                .windDirectionMax(270.0)
                .build();
        // 0 degrees is outside [90, 270]
        assertFalse(windChecker.isWindDirectionWithinLimits(item(0.0, 1.0, 1.0), cond));
    }
}
