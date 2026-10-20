package smhy.ratings;

import org.springframework.stereotype.Component;
import smhy.conditions.ForecastCondition;
import smhy.forecasts.WeatherForecast;
import smhy.locations.ForecastLocation;
import smhy.sun.SunCalculator;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Optional;

/**
 * Checks that a forecast hour falls inside the condition's clock window and inside daylight.
 * <p>
 * Daylight is sunrise–sunset for that hour's local date at the site. When the forecast has
 * coordinates, sunrise and sunset are calculated per day. Otherwise the sunrise and sunset
 * stored on the forecast are used. An hour has to satisfy both the clock window and daylight.
 */
@Component
public class TimeOfDayChecker {

    private static final ZoneId ZONE = SunCalculator.ZONE;
    private final SunCalculator sunCalculator = new SunCalculator();

    /**
     * @param forecast required; non null.
     * @param forecastItem required; non null.
     * @param condition required; non null.
     * @return true when the hour is inside the condition window and inside daylight.
     */
    public boolean isTimeOfDayWithinLimits(
            WeatherForecast forecast,
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {
        return isAfterTimeOfDayMin(forecastItem, condition)
                && isBeforeTimeOfDayMax(forecastItem, condition)
                && isWithinDaylight(forecast, forecastItem);
    }

    private boolean isAfterTimeOfDayMin(
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {
        if (condition.getTimeOfDayMin() == null) {
            return true;
        }
        return getHourOfDay(forecastItem) >= condition.getTimeOfDayMin();
    }

    private boolean isBeforeTimeOfDayMax(
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {
        if (condition.getTimeOfDayMax() == null) {
            return true;
        }
        return getHourOfDay(forecastItem) <= condition.getTimeOfDayMax();
    }

    private boolean isWithinDaylight(WeatherForecast forecast, WeatherForecast.WeatherForecastItem forecastItem) {
        Optional<SunCalculator.Span> sun = sunFor(forecast, forecastItem);
        if (sun.isEmpty()) {
            return true;
        }
        ZonedDateTime local = forecastItem.getTime().atZone(ZONE);
        LocalTime time = local.toLocalTime();
        return !time.isBefore(sun.get().rise()) && time.isBefore(sun.get().set());
    }

    private Optional<SunCalculator.Span> sunFor(WeatherForecast forecast, WeatherForecast.WeatherForecastItem forecastItem) {
        ForecastLocation location = forecast.getForecastLocation();
        if (location != null && location.getLatitude() != null && location.getLongitude() != null) {
            try {
                double latitude = Double.parseDouble(location.getLatitude());
                double longitude = Double.parseDouble(location.getLongitude());
                ZonedDateTime local = forecastItem.getTime().atZone(ZONE);
                return sunCalculator.on(latitude, longitude, local.toLocalDate());
            } catch (NumberFormatException ignored) {
                // Fall through to the forecast-level times.
            }
        }
        if (forecast.getSunrise() != null && forecast.getSunset() != null) {
            return Optional.of(new SunCalculator.Span(forecast.getSunrise(), forecast.getSunset()));
        }
        return Optional.empty();
    }

    static int getHourOfDay(WeatherForecast.WeatherForecastItem forecastItem) {
        return forecastItem.getTime().atZone(ZONE).get(ChronoField.HOUR_OF_DAY);
    }
}
