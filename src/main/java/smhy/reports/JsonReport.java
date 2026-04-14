package smhy.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonReport {
    private Instant generatedAt;
    private String reportType;
    private List<LocationReport> locations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationReport {
        private String id;
        private String name;
        private String region;
        private Coordinates coordinates;
        private Boolean favorite;
        private List<String> takeoffs;
        private ForecastData forecast;
        private List<RatingData> ratings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinates {
        private String latitude;
        private String longitude;
        private String altitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastData {
        private String provider;
        private Instant forecastDate;
        private LocalTime sunrise;
        private LocalTime sunset;
        private List<ForecastItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastItem {
        private Instant time;
        private Double windSpeed;
        private Double windDirection;
        private Double windGusts;
        private Double airPressure;
        private Double airTemperature;
        private Double dewPointTemperature;
        private Double relativeHumidity;
        private Double precipitationAmount;
        private Double precipitationAmountMin;
        private Double precipitationAmountMax;
        private Double probabilityPrecipitation;
        private Double probabilityThunder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingData {
        private Instant time;
        private Double score;
        private ConditionData conditions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionData {
        private Double windDirectionMin;
        private Double windDirectionMax;
        private Double windSpeedMin;
        private Double windSpeedMax;
        private Double windGustMax;
        private Double airTemperatureMin;
        private Double airTemperatureMax;
        private Double relativeHumidityMin;
        private Double relativeHumidityMax;
        private Double airPressureMin;
        private Double airPressureMax;
        private Integer timeOfDayMin;
        private Integer timeOfDayMax;
        private Double hoursMin;
        private Double probabilityPrecipitationMin;
        private Double probabilityPrecipitationMax;
        private Double precipitationAmountMin;
        private Double precipitationAmountMax;
        private Double dewPointTemperatureMin;
        private Double dewPointTemperatureMax;
    }
}
