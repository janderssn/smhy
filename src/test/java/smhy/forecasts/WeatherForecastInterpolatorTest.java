package smhy.forecasts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

class WeatherForecastInterpolatorTest {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    private WeatherForecastInterpolator interpolator = new WeatherForecastInterpolator();

    @Test
    void testNumberOfHoursBetweenItems0() {
        int hoursBetweenItems = interpolator.numberOfHoursBetweenItems(
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T06:00:00Z"))
                        .build(),
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T06:00:00Z"))
                        .build());
        Assertions.assertEquals(0, hoursBetweenItems);
    }

    @Test
    void testNumberOfHoursBetweenItems1() {
        int hoursBetweenItems = interpolator.numberOfHoursBetweenItems(
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T06:00:00Z"))
                        .build(),
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T07:00:00Z"))
                        .build());
        Assertions.assertEquals(1, hoursBetweenItems);
    }

    @Test
    void testNumberOfHoursBetweenItems6() {
        int hoursBetweenItems = interpolator.numberOfHoursBetweenItems(
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T06:00:00Z"))
                        .build(),
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T12:00:00Z"))
                        .build());
        Assertions.assertEquals(6, hoursBetweenItems);
    }

    @Test
    void testAddNumberOfHoursToDate0() {
        Instant expected = convertDate("2022-11-02T06:00:00Z");
        Instant actual = interpolator.addNumberOfHoursToDate(convertDate("2022-11-02T06:00:00Z"), 0);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testAddNumberOfHoursToDate1() {
        Instant expected = convertDate("2022-11-02T07:00:00Z");
        Instant actual = interpolator.addNumberOfHoursToDate(convertDate("2022-11-02T06:00:00Z"), 1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testInterpolate1Hours() {
        List<WeatherForecast.WeatherForecastItem> result = interpolator.interpolate(List.of(
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T06:00:00Z"))
                        .windSpeed(1.0)
                        .build(),
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T07:00:00Z"))
                        .windSpeed(2.0)
                        .build()));
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T06:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(0));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T07:00:00Z"))
                .windSpeed(2.0)
                .build(), result.get(1));
    }

    @Test
    void testInterpolate6Hours() {
        List<WeatherForecast.WeatherForecastItem> result = interpolator.interpolate(List.of(
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T06:00:00Z"))
                        .windSpeed(1.0)
                        .build(),
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T12:00:00Z"))
                        .windSpeed(2.0)
                        .build()));
        Assertions.assertEquals(7, result.size());
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T06:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(0));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T07:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(1));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T08:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(2));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T09:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(3));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T10:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(4));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T11:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(5));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T12:00:00Z"))
                .windSpeed(2.0)
                .build(), result.get(6));
    }

    @Test
    void testInterpolate12Hours() {
        List<WeatherForecast.WeatherForecastItem> result = interpolator.interpolate(List.of(
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T06:00:00Z"))
                        .windSpeed(1.0)
                        .build(),
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T12:00:00Z"))
                        .windSpeed(2.0)
                        .build(),
                WeatherForecast.WeatherForecastItem.builder()
                        .time(convertDate("2022-11-02T18:00:00Z"))
                        .windSpeed(3.0)
                        .build()));
        Assertions.assertEquals(13, result.size());
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T06:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(0));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T07:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(1));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T11:00:00Z"))
                .windSpeed(1.0)
                .build(), result.get(5));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T12:00:00Z"))
                .windSpeed(2.0)
                .build(), result.get(6));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T13:00:00Z"))
                .windSpeed(2.0)
                .build(), result.get(7));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T17:00:00Z"))
                .windSpeed(2.0)
                .build(), result.get(11));
        Assertions.assertEquals(WeatherForecast.WeatherForecastItem.builder()
                .time(convertDate("2022-11-02T18:00:00Z"))
                .windSpeed(3.0)
                .build(), result.get(12));
    }

    private Instant convertDate(String jsonDate) {
        return Instant.from(dateTimeFormatter.parse(jsonDate));
    }
}
