package smhy.ratings;

import org.springframework.stereotype.Component;
import smhy.conditions.ForecastCondition;
import smhy.forecasts.WeatherForecast;

import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * Utility class used to check if a forecastItem is within the specified calendar period.
 */
@Component
public class CalendarChecker {

    /**
     * Returns true if the forecastItem is within the specified calndar period of the given by condition.
     * i.e.
     * <pre>
     * forecastItem.time.dayOfWeek in condition.daysOfWeek
     * forecastITem.time.monthOfYear in condition.monthsOfYear
     * </pre>
     * @param forecastItem required; not null
     * @param condition required; not null
     * @return true if forecastItem is within calendar period, otherwise false.
     */
    public boolean isCalendarTimeWithinLimits(
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {

        int dayOfWeek = getDayOfWeek(forecastItem);
        int monthOfYear = getMonthOfYear(forecastItem);

        if (!matches(condition.getDaysOfWeek(), dayOfWeek)) {
            return false;
        }

        if (!matches(condition.getMonthsOfYear(), monthOfYear)) {
            return false;
        }

        return true;
    }

    int getDayOfWeek(WeatherForecast.WeatherForecastItem f) {
        return f.getTime().atZone(ZoneId.of("Europe/Stockholm")).get(ChronoField.DAY_OF_WEEK);
    }

    int getMonthOfYear(WeatherForecast.WeatherForecastItem f) {
        return f.getTime().atZone(ZoneId.of("Europe/Stockholm")).get(ChronoField.MONTH_OF_YEAR);
    }

    private <T> boolean matches(List<T> list, T object) {
        if (list == null || list.isEmpty()) {
            // null or empty list means that check should be skipped.
            return true;
        }
        return list.contains(object);
    }
}
