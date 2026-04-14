package smhy.forecasts.smhi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import smhy.forecasts.WeatherForecast;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.stream.StreamSupport;


@Slf4j
@Component
public class SmhiWeatherForecastConverter implements Converter<ObjectNode, WeatherForecast> {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Override
    public @NonNull WeatherForecast convert(ObjectNode objectNode) {
        WeatherForecast.WeatherForecastBuilder builder = WeatherForecast.builder();
        String updatedAtStr = objectNode.at("/referenceTime").textValue();
        builder.forecastDate(convertDate(updatedAtStr));
        builder.items(StreamSupport.stream(objectNode.at("/timeSeries").spliterator(), false)
                .map(this::convertItem)
                .toList());
        return builder.build();
    }

    private Instant convertDate(String jsonDate) {
        return Instant.from(dateTimeFormatter.parse(jsonDate));
    }

    private WeatherForecast.WeatherForecastItem convertItem(JsonNode node) {
        WeatherForecast.WeatherForecastItem forecastItem = WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate(node.get("validTime").textValue()))
                .probabilityPrecipitation(0d)
                .build();
        StreamSupport.stream(node.at("/parameters").spliterator(), false)
                .forEach(param -> applyParam(forecastItem, param));
        return forecastItem;

    }

    private void applyParam(WeatherForecast.WeatherForecastItem forecastItem, JsonNode param) {
        double value = valueOf(param);
        switch (nameOf(param)) {
            case "t" -> forecastItem.setAirTemperature(value);
            case "wd" -> forecastItem.setWindDirection(value);
            case "ws" -> forecastItem.setWindSpeed(value);
            case "gust" -> forecastItem.setWindGusts(value);
            case "msl" -> forecastItem.setAirPressure(value);
            case "pmean" -> forecastItem.setPrecipitationAmount(value);
            case "pmin" -> forecastItem.setPrecipitationAmountMin(value);
            case "pmax" -> forecastItem.setPrecipitationAmountMax(value);
            case "tstm" -> forecastItem.setProbabilityThunder(value);
            case "r" -> forecastItem.setRelativeHumidity(value);
            default -> {}
        }
    }

    private String nameOf(JsonNode param) {
        return param.get("name").asText();
    }

    private double valueOf(JsonNode param) {
        return param.get("values").get(0).asDouble();
    }
}
