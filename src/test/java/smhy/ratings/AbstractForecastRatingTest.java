package smhy.ratings;

import smhy.conditions.ForecastCondition;
import smhy.conditions.ForecastConditionYamlFileStore;
import smhy.forecasts.WeatherForecast;

import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;

abstract class AbstractForecastRatingTest {

    ForecastRatingService forecastRatingService;
    Collection<ForecastCondition> conditions;
    WeatherForecast weatherForecast;
    WeatherForecast.WeatherForecastItem.WeatherForecastItemBuilder forecastBuilder;
    ForecastCondition.ForecastConditionBuilder conditionBuilder;
    Clock clock;

    void setUp() {
        clock = Clock.fixed(Instant.parse("2022-07-01T08:00:00Z"), ZoneOffset.UTC);
        ForecastConditionYamlFileStore conditionStore = new ForecastConditionYamlFileStore();
        conditions = conditionStore.load(Paths.get("src/main/resources/config/conditions.yml"));
        weatherForecast = WeatherForecast.builder().build();
        forecastBuilder = WeatherForecast.WeatherForecastItem.builder()
                .time(LocalDateTime.parse("2022-07-01T13:00:00").toInstant(ZoneOffset.UTC))
                .windDirection(0.0)
                .windSpeed(1.0)
                .windGusts(2.0)
                .airTemperature(1.0)
                .dewPointTemperature(1.0)
                .precipitationAmount(0.0)
                .precipitationAmountMin(0.0)
                .precipitationAmountMax(0.0)
                .probabilityThunder(0.0)
                .probabilityPrecipitation(0.0)
                .airPressure(1013.0);

        conditionBuilder = ForecastCondition.builder()
                .id("test")
                .location("test")
                .score(1.0)
                .windSpeedMin(0.0)
                .windSpeedMax(100.0)
                .windGustMax(100.0)
                .hoursMin(1);

        forecastRatingService = new ForecastRatingService(
                null,
                null,
                null,
                null,
                new WindChecker(new DegreeToDirectionConverter(), new DirectionBoundsChecker()),
                new TimeOfDayChecker(),
                new CalendarChecker());
    }

    Collection<ForecastCondition> conditionsFor(String location) {
        return conditions.stream().filter(c -> location.equals(c.getLocation())).toList();
    }

    WeatherForecast.WeatherForecastItem windFrom(double windSpeed, double direction) {
        return forecastBuilder
                .windDirection(direction)
                .windSpeed(windSpeed)
                .windGusts(0.0)
                .build();
    }

    WeatherForecast.WeatherForecastItem weakWindFrom(double direction) {
        return forecastBuilder
                .windDirection(direction)
                .windSpeed(1.0)
                .windGusts(2.0)
                .build();
    }

    WeatherForecast.WeatherForecastItem forecastModerateWindFrom(double direction) {
        return forecastBuilder
                .windDirection(direction)
                .windSpeed(4.0)
                .windGusts(8.0)
                .build();
    }

    WeatherForecast.WeatherForecastItem forecastStrongWindFrom(double direction) {
        return forecastBuilder
                .windDirection(direction)
                .windSpeed(8.0)
                .windGusts(12.0)
                .build();
    }
    double score(Collection<ForecastCondition> conditions,
                 WeatherForecast.WeatherForecastItem forecastItem) {
        return forecastRatingService
                .rateForecastItemUsingConditions(weatherForecast, forecastItem, conditions)
                .map(ForecastRating.SiteRating::getScore)
                .orElse(0.0);
    }

    double score(Collection<ForecastCondition> conditions,
                 WeatherForecast forecast,
                 WeatherForecast.WeatherForecastItem forecastItem) {
        return forecastRatingService
                .rateForecastItemUsingConditions(forecast, forecastItem, conditions)
                .map(ForecastRating.SiteRating::getScore)
                .orElse(0.0);
    }

    static final double STP = 22.5;
    static final double N   = 0.0;
    static final double NNE = N   + STP;
    static final double NE  = NNE + STP;
    static final double ENE = NE  + STP;
    static final double E   = 90;
    static final double ESE = E   + STP;
    static final double SE  = ESE + STP;
    static final double SSE = SE  + STP;
    static final double S   = 180;
    static final double SSW = S   + STP;
    static final double SW  = SSW + STP;
    static final double WSW = SW  + STP;
    static final double W   = 270;
    static final double WNW = W   + STP;
    static final double NW  = WNW + STP;
    static final double NNW = NW  + STP;

}