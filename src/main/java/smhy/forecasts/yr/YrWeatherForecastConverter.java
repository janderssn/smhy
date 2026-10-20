package smhy.forecasts.yr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.lang.NonNull;
import smhy.forecasts.WeatherForecast;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.function.Predicate.not;


@Component
public class YrWeatherForecastConverter implements Converter<ObjectNode, WeatherForecast> {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Override
    public @NonNull WeatherForecast convert(ObjectNode objectNode) {
        WeatherForecast.WeatherForecastBuilder builder = WeatherForecast.builder();
        String updatedAtStr = objectNode.at("/properties/meta/updated_at").textValue();
        builder.forecastDate(convertDate(updatedAtStr));
        builder.items(StreamSupport.stream(objectNode.at("/properties/timeseries").spliterator(), false)
                .map(this::convertItem)
                .toList());
        return builder.build();
    }

    private Instant convertDate(String jsonDate) {
        return Instant.from(dateTimeFormatter.parse(jsonDate));
    }

    private WeatherForecast.WeatherForecastItem convertItem(JsonNode node) {
        JsonNode details = node.at("/data/instant/details");
        JsonNode nextOneHours = node.at("/data/next_1_hours/details");
        JsonNode nextSixHours = node.at("/data/next_6_hours/details");
        JsonNode nextTwelveHours = node.at("/data/next_12_hours/details");
        JsonNode next = Stream.of(nextOneHours, nextSixHours, nextTwelveHours)
                .filter(not(JsonNode::isMissingNode))
                .findFirst()
                .orElse(JsonNodeFactory.instance.missingNode());
        return WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate(node.get("time").textValue()))
                .windDirection(details.get("wind_from_direction").asDouble())
                .windGusts(details.hasNonNull("wind_speed_of_gust") ? details.get("wind_speed_of_gust").asDouble() : null)
                .windSpeed(details.get("wind_speed").asDouble())
                .airPressure(details.get("air_pressure_at_sea_level").asDouble())
                .airTemperature(details.get("air_temperature").asDouble())
                .relativeHumidity(details.get("relative_humidity").asDouble())
                .dewPointTemperature(details.get("dew_point_temperature").asDouble())
                .precipitationAmount(next.at("/precipitation_amount").asDouble())
                .precipitationAmountMin(next.at("/precipitation_amount_min").asDouble())
                .precipitationAmountMax(next.at("/precipitation_amount_max").asDouble())
                .probabilityPrecipitation(next.at("/probability_of_precipitation").asDouble())
                .probabilityThunder(next.at("/probability_of_thunder").asDouble())
                .build();
    }
}
