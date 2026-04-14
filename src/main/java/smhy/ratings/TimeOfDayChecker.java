package smhy.ratings;

import org.springframework.stereotype.Component;
import smhy.conditions.ForecastCondition;
import smhy.forecasts.WeatherForecast;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

/**
 * Utility class used to check if a forecastItem is within the specified condition time span.
 */
@Component
public class TimeOfDayChecker {

    /**
     * Returns true if the given forecastItem is within the time span of the provided condition.
     * The forecastItem is within the time span if:
     * <pre>
     * forecastItem.time >= condition.timeOfDayMin
     * forecastItem.time <= condition.timeOfDayMax
     * forecastItem.time > forcast.sunrise
     * forecastItem.time < forecast.sunset
     * </pre>
     * Note: timeOfDayMin and timeOfDayMax will take precedence over sunrise and sunset.
     * if condition.timeOfDayMin is null then forecast.sunrise is used.
     * if condition.timeOfDayMax is null then forecast.sunset is used.
     *
     * @param forecast required; non null.
     * @param forecastItem required; non null.
     * @param condition required; non null.
     * @return True if forecastItem is within time span provided by conditions, otherwise false.
     */
    public boolean isTimeOfDayWithinLimits(
            WeatherForecast forecast,
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {
        return isAfterTimeOfDayMinOrSunrise(forecast, forecastItem, condition)
                && isBeforeTimeOfDayMaxOrSunset(forecast, forecastItem, condition);
    }

    protected boolean isAfterTimeOfDayMinOrSunrise(
            WeatherForecast forecast,
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {
        int hourOfDay = getHourOfDay(forecastItem);
        if (condition.getTimeOfDayMin() != null) {
            return hourOfDay >= condition.getTimeOfDayMin();
        }
        if (forecast.getSunrise() != null) {
            return LocalTime.from(forecastItem.getTime().atZone(ZoneId.of("Europe/Stockholm"))).isAfter(forecast.getSunrise());
        }
        return true;
    }

    protected boolean isBeforeTimeOfDayMaxOrSunset(
            WeatherForecast forecast,
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {
        int hourOfDay = getHourOfDay(forecastItem);
        if (condition.getTimeOfDayMax() != null) {
            return hourOfDay <= condition.getTimeOfDayMax();
        }
        if (forecast.getSunset() != null) {
            return LocalTime.from(forecastItem.getTime().atZone(ZoneId.of("Europe/Stockholm"))).isBefore(forecast.getSunset());
        }
        return true;
    }

    private int getHourOfDay(WeatherForecast.WeatherForecastItem f) {
        return f.getTime().atZone(ZoneId.of("Europe/Stockholm")).get(ChronoField.CLOCK_HOUR_OF_DAY);
    }

}
