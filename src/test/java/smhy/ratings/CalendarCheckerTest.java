package smhy.ratings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smhy.conditions.ForecastCondition;
import smhy.forecasts.WeatherForecast;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CalendarChecker}.
 *
 * Reference time: 2022-07-01T13:00:00Z
 * In Europe/Stockholm (UTC+2 in summer) that is 15:00 local time.
 * Day of week: Friday = 5, Month of year: July = 7.
 */
class CalendarCheckerTest {

    private CalendarChecker calendarChecker;

    // 2022-07-01T13:00:00Z = Friday, July 1 2022 at 15:00 Stockholm time
    private static final Instant FRIDAY_JULY = Instant.parse("2022-07-01T13:00:00Z");

    @BeforeEach
    void setUp() {
        calendarChecker = new CalendarChecker();
    }

    private WeatherForecast.WeatherForecastItem itemAt(Instant time) {
        return WeatherForecast.WeatherForecastItem.builder().time(time).build();
    }

    // --- daysOfWeek ---

    @Test
    void isCalendarTimeWithinLimits_passesWhenDaysOfWeekIsNull() {
        ForecastCondition cond = ForecastCondition.builder().daysOfWeek(null).build();
        assertTrue(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    @Test
    void isCalendarTimeWithinLimits_passesWhenDaysOfWeekIsEmpty() {
        ForecastCondition cond = ForecastCondition.builder().daysOfWeek(List.of()).build();
        assertTrue(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    @Test
    void isCalendarTimeWithinLimits_passesWhenDayMatchesDaysOfWeek() {
        // Friday = 5
        ForecastCondition cond = ForecastCondition.builder().daysOfWeek(List.of(5)).build();
        assertTrue(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    @Test
    void isCalendarTimeWithinLimits_failsWhenDayNotInDaysOfWeek() {
        // Saturday=6 and Sunday=7; forecast is on Friday=5
        ForecastCondition cond = ForecastCondition.builder().daysOfWeek(List.of(6, 7)).build();
        assertFalse(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    // --- monthsOfYear ---

    @Test
    void isCalendarTimeWithinLimits_passesWhenMonthsOfYearIsNull() {
        ForecastCondition cond = ForecastCondition.builder().monthsOfYear(null).build();
        assertTrue(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    @Test
    void isCalendarTimeWithinLimits_passesWhenMonthsOfYearIsEmpty() {
        ForecastCondition cond = ForecastCondition.builder().monthsOfYear(List.of()).build();
        assertTrue(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    @Test
    void isCalendarTimeWithinLimits_passesWhenMonthMatchesMonthsOfYear() {
        // July = 7
        ForecastCondition cond = ForecastCondition.builder().monthsOfYear(List.of(7)).build();
        assertTrue(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    @Test
    void isCalendarTimeWithinLimits_failsWhenMonthNotInMonthsOfYear() {
        // Jan=1, Feb=2, Mar=3; forecast is in July=7
        ForecastCondition cond = ForecastCondition.builder()
                .daysOfWeek(List.of(5))        // matches Friday, so day check passes
                .monthsOfYear(List.of(1, 2, 3))
                .build();
        assertFalse(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }

    // --- combined ---

    @Test
    void isCalendarTimeWithinLimits_passesWhenBothMatch() {
        ForecastCondition cond = ForecastCondition.builder()
                .daysOfWeek(List.of(5))    // Friday
                .monthsOfYear(List.of(7))  // July
                .build();
        assertTrue(calendarChecker.isCalendarTimeWithinLimits(itemAt(FRIDAY_JULY), cond));
    }
}
