package smhy.ratings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import smhy.conditions.ForecastCondition;
import smhy.conditions.ForecastConditionService;
import smhy.forecasts.WeatherForecast;
import smhy.forecasts.WeatherForecastInterpolator;
import org.springframework.stereotype.Service;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;

import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForecastRatingService {

    private final ForecastConditionService forecastConditionService;
    private final ForecastLocationService forecastLocationService;
    private final ForecastRatingAggregator forecastRatingAggregator;
    private final WeatherForecastInterpolator forecastInterpolator;
    private final WindChecker windChecker;
    private final TimeOfDayChecker timeOfDayChecker;
    private final CalendarChecker calendarChecker;

    public ForecastRating rateForecast(WeatherForecast forecast) {

        List<ForecastCondition> conditions = forecastConditionService.findByLocation(forecast.getForecastLocation().getId());
        List<WeatherForecast.WeatherForecastItem> items = forecastInterpolator.interpolate(forecast.getItems());
        List<ForecastRating.SiteRating> ratings = items.stream()
                .flatMap(forecastItem -> rateForecastItemUsingConditions(forecast, forecastItem, conditions).stream())
                .toList();


        Collection<ForecastRating.SiteRating> aggregateRatings = forecastRatingAggregator.aggregateRatings(ratings);
        aggregateRatings.forEach(this::prettyfy);

        return ForecastRating.builder()
                .forecastDate(forecast.getForecastDate())
                .forecastLocation(forecast.getForecastLocation().getId())
                .siteRatings(aggregateRatings)
                .build();
    }

    Optional<ForecastRating.SiteRating> rateForecastItemUsingConditions(
            WeatherForecast forecast,
            WeatherForecast.WeatherForecastItem forecastItem,
            Collection<ForecastCondition> conditions) {

        return conditions.stream()
                .map(forecastCondition -> rateForecastItemUsingCondition(forecast, forecastItem, forecastCondition))
                .reduce(this::max);
    }

    ForecastRating.SiteRating rateForecastItemUsingCondition(
            WeatherForecast forecast,
            WeatherForecast.WeatherForecastItem forecastItem,
            ForecastCondition condition) {

        int hourOfDay = getHourOfDay(forecastItem);

        ForecastRating.SiteRating.SiteRatingBuilder builder = ForecastRating.SiteRating.builder()
                .location(condition.getLocation())
                .time(forecastItem.getTime())
                .score(condition.getScore())
                .windDirectionMin(forecastItem.getWindDirection())
                .windDirectionMax(forecastItem.getWindDirection())
                .windSpeedMin(forecastItem.getWindSpeed())
                .windSpeedMax(forecastItem.getWindSpeed())
                .windGustMax(forecastItem.getWindGusts())
                .timeOfDayMin(hourOfDay)
                .timeOfDayMax(hourOfDay)
                .airTemperatureMin(forecastItem.getAirTemperature())
                .airTemperatureMax(forecastItem.getAirTemperature())
                .airPressureMin(forecastItem.getAirPressure())
                .airPressureMax(forecastItem.getAirPressure())
                .relativeHumidityMin(forecastItem.getRelativeHumidity())
                .relativeHumidityMax(forecastItem.getRelativeHumidity())
                .dewPointTemperatureMin(forecastItem.getDewPointTemperature())
                .dewPointTemperatureMax(forecastItem.getDewPointTemperature())
                .precipitationAmountMin(forecastItem.getPrecipitationAmountMin())
                .precipitationAmountMax(forecastItem.getPrecipitationAmountMax())
                .probabilityPrecipitationMin(forecastItem.getProbabilityPrecipitation())
                .probabilityPrecipitationMax(forecastItem.getProbabilityPrecipitation())
                .hoursMin(1.0);

        if (!calendarChecker.isCalendarTimeWithinLimits(forecastItem, condition)) {
            return builder.score(0d).build();
        }

        if (!timeOfDayChecker.isTimeOfDayWithinLimits(forecast, forecastItem, condition)) {
            return builder.score(0d).build();
        }

        if (!windChecker.isWindDirectionWithinLimits(forecastItem, condition)) {
            return builder.score(0d).build();
        }

        if (!windChecker.isWindSpeedWithinLimits(forecastItem, condition)) {
            return builder.score(0d).build();
        }

        if (forecastItem.getPrecipitationAmount() > 0
                || forecastItem.getPrecipitationAmountMax() > 0
                || forecastItem.getProbabilityPrecipitation() > 50) {
            return builder.score(0d).build();
        }

        return builder.score(condition.getScore()).build();
    }

    int getHourOfDay(WeatherForecast.WeatherForecastItem f) {
        return f.getTime().atZone(ZoneOffset.UTC).get(ChronoField.CLOCK_HOUR_OF_DAY);
    }

    private ForecastRating.SiteRating max(ForecastRating.SiteRating a, ForecastRating.SiteRating b) {
        return a.score >= b.score ? a : b;
    }

    /**
     * Sets the location name of the SiteRating to name of the ForecastLocation.
     * @param siteRating required; non-null
     */
    private void prettyfy(ForecastRating.SiteRating siteRating) {
        String locationName = forecastLocationService.findById(siteRating.getLocation())
                .map(ForecastLocation::getName)
                .orElse(siteRating.getLocation());
        siteRating.setName(locationName);
    }
}
