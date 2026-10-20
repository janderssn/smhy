package smhy.reports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import smhy.dashboard.DashboardService;
import smhy.forecasts.InMemoryForecastStore;
import smhy.forecasts.WeatherForecast;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;
import smhy.ratings.ForecastRating;
import smhy.ratings.ForecastRatingService;
import smhy.util.ResourceCopyUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastReportGenerator {

    private final ForecastLocationService forecastLocationService;
    private final InMemoryForecastStore forecastStore;
    private final ForecastRatingService forecastRatingService;
    private final ForecastReportHtmlTemplate forecastReportHtmlTemplate;
    private final ForecastReportJsonGenerator forecastReportJsonGenerator;
    private final DashboardService dashboardService;

    @Value("${smhy.output.dir}")
    private PathResource htmlOutputDirectory;

    @Value("${smhy.assets.dir}")
    private Resource htmlAssetsDirectory;

    @Value("${smhy.region:}")
    private String region;

    @Value("${smhy.provider:yr}")
    private String provider = "yr";

    public void generate(List<String> locationIds) {

        Function<ForecastLocation, Optional<WeatherForecast>> findLatestForecastByLocationAndProvider = location ->
            forecastStore.findLatestByLocation(location.getId(), provider);

        Collection<ForecastRating.SiteRating> ratings = forecastLocationService.findAll().stream()
                .filter(l -> isEmpty(locationIds) || locationIds.contains(l.getId()) )
                .map(findLatestForecastByLocationAndProvider)
                .flatMap(Optional::stream)
                .map(forecastRatingService::rateForecast)
                .flatMap(r -> r.getSiteRatings().stream())
                .sorted(Comparator.comparing(ForecastRating.SiteRating::getTime))
                .collect(Collectors.toList());

        generate(ratings);
    }

    public void generate(Collection<ForecastRating.SiteRating> ratings) {

        try {
            Path htmlOutputPath = Paths.get(htmlOutputDirectory.getPath());
            Files.createDirectories(htmlOutputPath);
            ResourceCopyUtils.copyDirectory(htmlAssetsDirectory, htmlOutputPath.resolve("assets"));
            String regionHtml = getRegionName().toLowerCase().concat(".html");
            forecastReportHtmlTemplate.generate(getAll(ratings), htmlOutputPath.resolve("index.html"));
            forecastReportHtmlTemplate.generate(getWeekend(ratings), htmlOutputPath.resolve("helg.html"));
            forecastReportHtmlTemplate.generate(getFavorites(ratings), htmlOutputPath.resolve("favoriter.html"));
            forecastReportHtmlTemplate.generate(getRegion(ratings), htmlOutputPath.resolve(regionHtml));
        } catch (IOException e) {
            log.warn("failed to create html", e);
        }

        try {
            Path htmlOutputPath = Paths.get(htmlOutputDirectory.getPath());
            Files.createDirectories(htmlOutputPath);
            dashboardService.write(htmlOutputPath);
        } catch (IOException e) {
            log.warn("failed to write dashboard", e);
        }

        // Generate JSON reports
        forecastReportJsonGenerator.generate("all", getAll(ratings).getSiteRatings());
        forecastReportJsonGenerator.generate("weekend", getWeekend(ratings).getSiteRatings());
        forecastReportJsonGenerator.generate("favorites", getFavorites(ratings).getSiteRatings());
        forecastReportJsonGenerator.generate(getRegionName(), getRegion(ratings).getSiteRatings());

    }

    private ForecastRating getAll(Collection<ForecastRating.SiteRating> ratings) {
        return ForecastRating.builder()
                .forecastDate(Instant.now())
                .forecastLocation("Vecka")
                .siteRatings(ratings)
                .build();
    }

    private ForecastRating getWeekend(Collection<ForecastRating.SiteRating> ratings) {
        return ForecastRating.builder()
                .forecastDate(Instant.now())
                .forecastLocation("Helg")
                .siteRatings(ratings.stream().filter(this::isInWeekend).collect(Collectors.toList()))
                .build();
    }

    private ForecastRating getFavorites(Collection<ForecastRating.SiteRating> ratings) {
        return ForecastRating.builder()
                .forecastDate(Instant.now())
                .forecastLocation("Favoriter")
                .siteRatings(ratings.stream().filter(this::isInFavorites).collect(Collectors.toList()))
                .build();
    }

    private boolean isInFavorites(ForecastRating.SiteRating siteRating) {
        return forecastLocationService.findById(siteRating.getLocation())
                .map(ForecastLocation::getFavorite)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    private ForecastRating getRegion(Collection<ForecastRating.SiteRating> ratings) {
        return ForecastRating.builder()
                .forecastDate(Instant.now())
                .forecastLocation(getRegionName())
                .siteRatings(ratings.stream().filter(this::isInRegion).collect(Collectors.toList()))
                .build();
    }

    private String getRegionName() {
        return StringUtils.capitalize(region.replaceFirst(".*[/]",""));
    }

    private boolean isInRegion(ForecastRating.SiteRating siteRating) {
        return forecastLocationService.findById(siteRating.getLocation())
                .map(ForecastLocation::getRegion)
                .map(region::equalsIgnoreCase)
                .orElse(false);
    }

    private boolean isInWeekend(ForecastRating.SiteRating siteRating) {
        return isSaturdayOrSunday(siteRating.getTime());
    }

    private boolean isSaturdayOrSunday(Instant date) {
        return date.atZone(ZoneId.of("Europe/Stockholm")).get(ChronoField.DAY_OF_WEEK) >= 6;
    }

}
