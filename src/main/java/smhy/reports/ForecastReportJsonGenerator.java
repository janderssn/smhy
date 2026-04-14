package smhy.reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import smhy.forecasts.InMemoryForecastStore;
import smhy.forecasts.WeatherForecast;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;
import smhy.ratings.ForecastRating;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastReportJsonGenerator {

    private final ForecastLocationService forecastLocationService;
    private final InMemoryForecastStore forecastStore;
    private final ObjectMapper objectMapper;
    private final smhy.JsonExportConfiguration jsonExportConfiguration;

    @Value("${smhy.output.dir}")
    private PathResource jsonOutputDirectory;

    public void generate(String reportType, Collection<ForecastRating.SiteRating> ratings) {
        if (!jsonExportConfiguration.isEnabled()) {
            log.debug("JSON generation is disabled");
            return;
        }

        try {
            Path jsonOutputPath = Paths.get(jsonOutputDirectory.getPath());
            Files.createDirectories(jsonOutputPath);

            JsonReport report = buildJsonReport(reportType, ratings);

            String filename = reportType.toLowerCase().replace(" ", "-") + ".json";
            Path outputFile = jsonOutputPath.resolve(filename);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            mapper.writeValue(outputFile.toFile(), report);

            log.info("Generated JSON report: {}", outputFile);
        } catch (IOException e) {
            log.error("Failed to generate JSON report for {}", reportType, e);
        }
    }

    private JsonReport buildJsonReport(String reportType, Collection<ForecastRating.SiteRating> ratings) {
        Map<String, List<ForecastRating.SiteRating>> ratingsByLocation = ratings.stream()
                .collect(Collectors.groupingBy(ForecastRating.SiteRating::getLocation));

        List<JsonReport.LocationReport> locationReports = ratingsByLocation.entrySet().stream()
                .map(entry -> buildLocationReport(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return JsonReport.builder()
                .generatedAt(Instant.now())
                .reportType(reportType)
                .locations(locationReports)
                .build();
    }

    private JsonReport.LocationReport buildLocationReport(String locationId, List<ForecastRating.SiteRating> ratings) {
        Optional<ForecastLocation> locationOpt = forecastLocationService.findById(locationId);
        if (locationOpt.isEmpty()) {
            log.warn("Location not found: {}", locationId);
            return null;
        }

        ForecastLocation location = locationOpt.get();
        Optional<WeatherForecast> forecastOpt = forecastStore.findLatestByLocation(locationId, "yr");

        JsonReport.ForecastData forecastData = forecastOpt
                .map(this::buildForecastData)
                .orElse(null);

        List<JsonReport.RatingData> ratingDataList = ratings.stream()
                .map(this::buildRatingData)
                .collect(Collectors.toList());

        return JsonReport.LocationReport.builder()
                .id(location.getId())
                .name(location.getName())
                .region(location.getRegion())
                .coordinates(JsonReport.Coordinates.builder()
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .altitude(location.getAltitude())
                        .build())
                .favorite(location.getFavorite())
                .takeoffs(location.getTakeoffs())
                .forecast(forecastData)
                .ratings(ratingDataList)
                .build();
    }

    private JsonReport.ForecastData buildForecastData(WeatherForecast forecast) {
        List<JsonReport.ForecastItem> items = forecast.getItems().stream()
                .map(item -> JsonReport.ForecastItem.builder()
                        .time(item.getTime())
                        .windSpeed(item.getWindSpeed())
                        .windDirection(item.getWindDirection())
                        .windGusts(item.getWindGusts())
                        .airPressure(item.getAirPressure())
                        .airTemperature(item.getAirTemperature())
                        .dewPointTemperature(item.getDewPointTemperature())
                        .relativeHumidity(item.getRelativeHumidity())
                        .precipitationAmount(item.getPrecipitationAmount())
                        .precipitationAmountMin(item.getPrecipitationAmountMin())
                        .precipitationAmountMax(item.getPrecipitationAmountMax())
                        .probabilityPrecipitation(item.getProbabilityPrecipitation())
                        .probabilityThunder(item.getProbabilityThunder())
                        .build())
                .collect(Collectors.toList());

        return JsonReport.ForecastData.builder()
                .provider(forecast.getProvider())
                .forecastDate(forecast.getForecastDate())
                .sunrise(forecast.getSunrise())
                .sunset(forecast.getSunset())
                .items(items)
                .build();
    }

    private JsonReport.RatingData buildRatingData(ForecastRating.SiteRating rating) {
        return JsonReport.RatingData.builder()
                .time(rating.getTime())
                .score(rating.getScore())
                .conditions(JsonReport.ConditionData.builder()
                        .windDirectionMin(rating.getWindDirectionMin())
                        .windDirectionMax(rating.getWindDirectionMax())
                        .windSpeedMin(rating.getWindSpeedMin())
                        .windSpeedMax(rating.getWindSpeedMax())
                        .windGustMax(rating.getWindGustMax())
                        .airTemperatureMin(rating.getAirTemperatureMin())
                        .airTemperatureMax(rating.getAirTemperatureMax())
                        .relativeHumidityMin(rating.getRelativeHumidityMin())
                        .relativeHumidityMax(rating.getRelativeHumidityMax())
                        .airPressureMin(rating.getAirPressureMin())
                        .airPressureMax(rating.getAirPressureMax())
                        .timeOfDayMin(rating.getTimeOfDayMin())
                        .timeOfDayMax(rating.getTimeOfDayMax())
                        .hoursMin(rating.getHoursMin())
                        .probabilityPrecipitationMin(rating.getProbabilityPrecipitationMin())
                        .probabilityPrecipitationMax(rating.getProbabilityPrecipitationMax())
                        .precipitationAmountMin(rating.getPrecipitationAmountMin())
                        .precipitationAmountMax(rating.getPrecipitationAmountMax())
                        .dewPointTemperatureMin(rating.getDewPointTemperatureMin())
                        .dewPointTemperatureMax(rating.getDewPointTemperatureMax())
                        .build())
                .build();
    }
}
