package smhy.forecasts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import smhy.locations.ForecastLocation;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecast {
    String id;
    String provider;
    ForecastLocation forecastLocation;
    Instant forecastDate;
    Instant expiresAt;
    LocalTime sunrise;
    LocalTime sunset;
    List<WeatherForecastItem> items;
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherForecastItem {
        String id;
        Instant time;
        Double windSpeed;
        Double windDirection;
        Double windGusts;
        Double airPressure;
        Double airTemperature;
        Double dewPointTemperature;
        Double relativeHumidity;
        Double precipitationAmount;
        Double precipitationAmountMin;
        Double precipitationAmountMax;
        Double probabilityPrecipitation;
        Double probabilityThunder;
    }
}
