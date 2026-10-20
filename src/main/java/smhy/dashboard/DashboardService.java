package smhy.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import smhy.forecasts.InMemoryForecastStore;
import smhy.forecasts.WeatherForecast;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;
import smhy.ratings.ForecastRating;
import smhy.ratings.ForecastRatingService;
import smhy.sun.SunCalculator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final int SCHEMA = 1;
    public static final int DAY_COUNT = 10;
    static final List<String> DEFAULT_FAVORITES = List.of("areskutan", "alleberg", "kaseberga");
    private static final String[] WEEKDAY = {"", "mån", "tis", "ons", "tor", "fre", "lör", "sön"};
    private static final String[] WEEKDAY_LONG = {"", "måndag", "tisdag", "onsdag", "torsdag", "fredag", "lördag", "söndag"};
    private static final String[] MONTH = {"", "jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"};
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ForecastLocationService forecastLocationService;
    private final InMemoryForecastStore forecastStore;
    private final ForecastRatingService forecastRatingService;
    private final SunCalculator sunCalculator;
    private final LaunchRules launchRules;
    private final ForecastSourceCatalog forecastSourceCatalog;

    @Value("${smhy.region:se/stockholm}")
    private String region = "se/stockholm";

    public Dashboard build() {
        return build(LocalDate.now(SunCalculator.ZONE));
    }

    public Dashboard build(LocalDate today) {
        Instant generatedAt = Instant.now();
        ZonedDateTime generated = generatedAt.atZone(SunCalculator.ZONE);
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < DAY_COUNT; i++) {
            dates.add(today.plusDays(i));
        }

        List<String> knownIds = forecastLocationService.findAll().stream().map(ForecastLocation::getId).toList();
        List<String> favorites = DEFAULT_FAVORITES.stream().filter(knownIds::contains).toList();

        List<Dashboard.Site> sites = forecastLocationService.findAll().stream()
                .map(location -> buildSite(location, dates, today))
                .toList();

        List<Dashboard.Day> days = new ArrayList<>();
        for (LocalDate date : dates) {
            boolean gusts = sites.stream()
                    .flatMap(site -> site.getHours().stream())
                    .anyMatch(hour -> date.toString().equals(hour.getDate()) && hour.getGust() != null);
            days.add(buildDay(date, today, gusts));
        }

        LocalDate last = dates.get(dates.size() - 1);
        String range = weekday(today) + " " + today.getDayOfMonth()
                + " – " + weekday(last) + " " + last.getDayOfMonth() + " " + month(last);

        return Dashboard.builder()
                .schema(SCHEMA)
                .generatedAt(generatedAt)
                .timezone(SunCalculator.ZONE.getId())
                .activeSource(forecastSourceCatalog.activeProvider())
                .updatedTime(generated.format(HM))
                .updatedDate(weekday(generated.toLocalDate()) + " " + generated.getDayOfMonth() + " " + month(generated.toLocalDate()))
                .rangeLabel(range)
                .regionView(region)
                .regionName(regionName(region))
                .defaultFavorites(favorites)
                .notes(List.of(
                        "Flygfönster är trimmade till lokal soluppgång och solnedgång.",
                        "Säkerhet är en prognoslängdsindikator, inte spridning mellan modeller.",
                        "Molnbas är beräknad ur temperatur minus daggpunkt (cirka 125 m per grad).",
                        "SMHI, Open-Meteo och Holfuy är inte inkopplade och har inga värden."))
                .sources(forecastSourceCatalog.sources())
                .days(days)
                .sites(sites)
                .build();
    }

    public void write(Path directory) throws IOException {
        Dashboard dashboard = build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Path output = directory.resolve("dashboard.json");
        mapper.writeValue(output.toFile(), dashboard);
        log.info("Generated dashboard {}", output);
    }

    private Dashboard.Site buildSite(ForecastLocation location, List<LocalDate> dates, LocalDate today) {
        String provider = forecastSourceCatalog.activeProvider();
        Optional<WeatherForecast> forecast = forecastStore.findLatestByLocation(location.getId(), provider);
        List<Dashboard.Sun> sun = dates.stream()
                .map(date -> sunFor(location, date))
                .toList();
        Dashboard.Launch launch = launchRules.forLocation(location.getId(), today.getMonthValue());

        List<Dashboard.Hour> hours = List.of();
        List<Dashboard.Window> windows = List.of();
        if (forecast.isPresent() && forecast.get().getItems() != null && !forecast.get().getItems().isEmpty()) {
            WeatherForecast weatherForecast = forecast.get();
            Set<Instant> originalTimes = new HashSet<>();
            weatherForecast.getItems().forEach(item -> originalTimes.add(item.getTime()));
            Set<LocalDate> dateSet = new HashSet<>(dates);
            List<ForecastRating.SiteRating> hourly = forecastRatingService.rateHourly(weatherForecast);
            hours = hourly.stream()
                    .map(rating -> toHour(rating, originalTimes, location))
                    .filter(hour -> dateSet.contains(LocalDate.parse(hour.getDate())))
                    .filter(hour -> hour.getHour() >= 4 && hour.getHour() <= 22)
                    .toList();
            windows = forecastRatingService.rateForecast(weatherForecast).getSiteRatings().stream()
                    .map(rating -> toWindow(rating, hourly, location))
                    .filter(window -> dateSet.contains(LocalDate.parse(window.getDate())))
                    .sorted(Comparator.comparing(Dashboard.Window::getDate)
                            .thenComparing(Dashboard.Window::getFromHour))
                    .toList();
        }

        return Dashboard.Site.builder()
                .id(location.getId())
                .name(location.getName())
                .region(location.getRegion())
                .regionName(regionName(location.getRegion()))
                .latitude(parseDouble(location.getLatitude()))
                .longitude(parseDouble(location.getLongitude()))
                .altitude((int) Math.round(parseDouble(location.getAltitude())))
                .holfuy(holfuyId(location.getStation()))
                .launch(launch)
                .sun(sun)
                .windows(windows)
                .hours(hours)
                .build();
    }

    private Dashboard.Hour toHour(ForecastRating.SiteRating rating, Set<Instant> originalTimes, ForecastLocation location) {
        ZonedDateTime local = rating.getTime().atZone(SunCalculator.ZONE);
        boolean daylight = isDaylight(location, local);
        return Dashboard.Hour.builder()
                .time(local.format(OFFSET))
                .date(local.toLocalDate().toString())
                .hour(local.getHour())
                .wind(round1(rating.getWindSpeedMax()))
                .gust(round1(rating.getWindGustMax()))
                .direction(round1(rating.getWindDirectionMax()))
                .temp(round1(rating.getAirTemperatureMax()))
                .dew(round1(rating.getDewPointTemperatureMax()))
                .precip(round1(rating.getPrecipitationAmountMax()))
                .precipMin(round1(rating.getPrecipitationAmountMin()))
                .precipMax(round1(rating.getPrecipitationAmountMax()))
                .precipProb(round1(rating.getProbabilityPrecipitationMax()))
                .pressure(round0(rating.getAirPressureMax()))
                .humidity(round0(rating.getRelativeHumidityMax()))
                .score(displayScore(rating.getScore()))
                .daylight(daylight)
                .repeated(!originalTimes.contains(rating.getTime()))
                .build();
    }

    private Dashboard.Window toWindow(ForecastRating.SiteRating rating,
                                      List<ForecastRating.SiteRating> hourly,
                                      ForecastLocation location) {
        ZonedDateTime start = rating.getTime().atZone(SunCalculator.ZONE);
        int from = rating.getTimeOfDayMin() == null ? start.getHour() : rating.getTimeOfDayMin();
        int to = rating.getTimeOfDayMax() == null ? from : rating.getTimeOfDayMax();
        String date = start.toLocalDate().toString();
        List<ForecastRating.SiteRating> inside = hourly.stream()
                .filter(hour -> {
                    ZonedDateTime local = hour.getTime().atZone(SunCalculator.ZONE);
                    return date.equals(local.toLocalDate().toString())
                            && local.getHour() >= from
                            && local.getHour() <= to
                            && hour.getScore() != null
                            && hour.getScore() > 0;
                })
                .toList();

        Double wind = average(inside, ForecastRating.SiteRating::getWindSpeedMax);
        Double gust = maxOf(inside, ForecastRating.SiteRating::getWindGustMax);
        Double direction = circularMean(inside);
        Double temp = average(inside, ForecastRating.SiteRating::getAirTemperatureMax);
        Double dew = average(inside, ForecastRating.SiteRating::getDewPointTemperatureMax);
        if (direction == null) {
            direction = rating.getWindDirectionMax();
        }
        return Dashboard.Window.builder()
                .date(date)
                .fromHour(from)
                .toHour(to)
                .durationHours(Math.max(1, to - from + 1))
                .score(Objects.requireNonNullElse(displayScore(rating.getScore()), 1))
                .scoreRaw(round1(rating.getScore()))
                .wind(round1(wind != null ? wind : rating.getWindSpeedMax()))
                .gust(round1(gust))
                .direction(round1(direction))
                .temp(round1(temp != null ? temp : rating.getAirTemperatureMax()))
                .dew(round1(dew != null ? dew : rating.getDewPointTemperatureMax()))
                .precipMin(round1(rating.getPrecipitationAmountMin()))
                .precipMax(round1(rating.getPrecipitationAmountMax()))
                .precipProb(round1(rating.getProbabilityPrecipitationMax()))
                .pressure(round0(average(inside, ForecastRating.SiteRating::getAirPressureMax)))
                .humidity(round0(average(inside, ForecastRating.SiteRating::getRelativeHumidityMax)))
                .cloudBase(cloudBase(temp, dew))
                .cloudBaseCalculated(temp != null && dew != null)
                .build();
    }

    private Dashboard.Day buildDay(LocalDate date, LocalDate today, boolean gusts) {
        int lead = (int) ChronoUnit.DAYS.between(today, date);
        int confidence = lead <= 0 && gusts ? 3 : lead <= 3 ? 2 : 1;
        String label = switch (confidence) {
            case 3 -> "Hög säkerhet";
            case 2 -> "Medel säkerhet";
            default -> "Låg säkerhet";
        };
        int dow = date.getDayOfWeek().getValue();
        return Dashboard.Day.builder()
                .date(date.toString())
                .dayOfMonth(date.getDayOfMonth())
                .weekday(WEEKDAY[dow])
                .weekdayLong(WEEKDAY_LONG[dow])
                .month(month(date))
                .weekend(dow >= 6)
                .today(date.equals(today))
                .leadDays(lead)
                .confidence(confidence)
                .confidenceLabel(label)
                .confidenceBasis("lead-time")
                .build();
    }

    private Dashboard.Sun sunFor(ForecastLocation location, LocalDate date) {
        return parseCoordinates(location)
                .flatMap(coords -> sunCalculator.on(coords[0], coords[1], date))
                .map(span -> Dashboard.Sun.builder()
                        .date(date.toString())
                        .rise(span.rise().format(HM))
                        .set(span.set().format(HM))
                        .build())
                .orElse(Dashboard.Sun.builder().date(date.toString()).build());
    }

    private boolean isDaylight(ForecastLocation location, ZonedDateTime local) {
        return parseCoordinates(location)
                .flatMap(coords -> sunCalculator.on(coords[0], coords[1], local.toLocalDate()))
                .map(span -> {
                    LocalTime time = local.toLocalTime();
                    return !time.isBefore(span.rise()) && time.isBefore(span.set());
                })
                .orElse(true);
    }

    static String regionName(String region) {
        if (region == null || region.isBlank()) {
            return "";
        }
        String slug = region;
        int slash = region.lastIndexOf('/');
        if (slash >= 0 && slash < region.length() - 1) {
            slug = region.substring(slash + 1);
        }
        Locale swedish = Locale.forLanguageTag("sv-SE");
        return slug.substring(0, 1).toUpperCase(swedish) + slug.substring(1);
    }

    private static Integer holfuyId(String station) {
        if (station == null || !station.startsWith("holfuy:")) {
            return null;
        }
        try {
            return Integer.valueOf(station.substring("holfuy:".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Optional<double[]> parseCoordinates(ForecastLocation location) {
        if (location.getLatitude() == null || location.getLongitude() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new double[]{
                    Double.parseDouble(location.getLatitude()),
                    Double.parseDouble(location.getLongitude())
            });
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Integer displayScore(Double raw) {
        if (raw == null || raw <= 0) {
            return null;
        }
        int score = (int) Math.round(raw / 10.0);
        return Math.max(1, Math.min(10, score));
    }

    private static Integer cloudBase(Double temp, Double dew) {
        if (temp == null || dew == null) {
            return null;
        }
        double metres = Math.max(0, (temp - dew) * 125.0);
        return (int) (Math.round(metres / 25.0) * 25);
    }

    private static Double average(List<ForecastRating.SiteRating> ratings,
                                  java.util.function.Function<ForecastRating.SiteRating, Double> getter) {
        double sum = 0;
        int n = 0;
        for (ForecastRating.SiteRating rating : ratings) {
            Double value = getter.apply(rating);
            if (value != null) {
                sum += value;
                n++;
            }
        }
        return n == 0 ? null : sum / n;
    }

    private static Double maxOf(List<ForecastRating.SiteRating> ratings,
                                java.util.function.Function<ForecastRating.SiteRating, Double> getter) {
        Double max = null;
        for (ForecastRating.SiteRating rating : ratings) {
            Double value = getter.apply(rating);
            if (value != null && (max == null || value > max)) {
                max = value;
            }
        }
        return max;
    }

    private static Double circularMean(List<ForecastRating.SiteRating> ratings) {
        double sin = 0;
        double cos = 0;
        int n = 0;
        for (ForecastRating.SiteRating rating : ratings) {
            if (rating.getWindDirectionMax() == null) {
                continue;
            }
            double radians = Math.toRadians(rating.getWindDirectionMax());
            sin += Math.sin(radians);
            cos += Math.cos(radians);
            n++;
        }
        if (n == 0) {
            return null;
        }
        double degrees = Math.toDegrees(Math.atan2(sin, cos));
        if (degrees < 0) {
            degrees += 360;
        }
        return degrees;
    }

    private static Double round1(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 10.0) / 10.0;
    }

    private static Double round0(Double value) {
        if (value == null) {
            return null;
        }
        return (double) Math.round(value);
    }

    private static String weekday(LocalDate date) {
        return WEEKDAY[date.getDayOfWeek().getValue()];
    }

    private static String month(LocalDate date) {
        return MONTH[date.getMonthValue()];
    }
}
