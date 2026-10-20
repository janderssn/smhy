package smhy.ratings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import smhy.conditions.ForecastCondition;
import smhy.conditions.WindDirection;
import smhy.forecasts.WeatherForecast;


import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Utility class used to check if a forecastItem is within the specified wind conditions.
 */
@Component
@RequiredArgsConstructor
public class WindChecker {

    private final DegreeToDirectionConverter degreeToDirectionConverter;
    private final DirectionBoundsChecker directionBoundsChecker;

    public boolean isWindDirectionWithinLimits(
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {
        if (condition.getWindDirectionMin() != null && condition.getWindDirectionMax() != null) {
            if (!directionBoundsChecker.isDirectionWithinBounds(
                    Math.round(forecastItem.getWindDirection()),
                    condition.getWindDirectionMin(),
                    condition.getWindDirectionMax())) {
                return false;
            }
        }

        if (!isEmpty(condition.getWindDirections())) {
            String direction = degreeToDirectionConverter.convert(forecastItem.getWindDirection());
            if (!condition.getWindDirections().contains(WindDirection.valueOf(direction))) {
                return false;
            }
        }

        return true;
    }

    public boolean isWindSpeedWithinLimits(
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {

        if (Math.round(forecastItem.getWindSpeed()) < condition.getWindSpeedMin()
                || Math.round(forecastItem.getWindSpeed()) > condition.getWindSpeedMax()) {
            return false;
        }

        Double gusts = forecastItem.getWindGusts();
        if (gusts != null && condition.getWindGustMax() != null
                && Math.round(gusts) > condition.getWindGustMax()) {
            return false;
        }

        return true;
    }
}
