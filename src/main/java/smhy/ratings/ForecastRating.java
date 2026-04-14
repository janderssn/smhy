package smhy.ratings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collection;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ForecastRating {
    String id;
    Instant forecastDate;
    String forecastLocation;
    Collection<SiteRating> siteRatings;

    @Data
    @Builder(toBuilder = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SiteRating {
        String location;
        String name;
        Instant time;
        Double score;
        Double windDirectionMin;
        Double windDirectionMax;
        Double windGustMax;
        Double windSpeedMin;
        Double windSpeedMax;
        Double airTemperatureMin;
        Double airTemperatureMax;
        Double relativeHumidityMin;
        Double relativeHumidityMax;
        Double airPressureMin;
        Double airPressureMax;
        Integer timeOfDayMin;
        Integer timeOfDayMax;
        Double hoursMin;
        Double probabilityPrecipitationMin;
        Double probabilityPrecipitationMax;
        Double precipitationAmountMin;
        Double precipitationAmountMax;
        Double dewPointTemperatureMin;
        Double dewPointTemperatureMax;
    }
}
