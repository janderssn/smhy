package smhy.ratings;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for grouping SiteRating:s suitable for display.
 */
@UtilityClass
public class ForecastRatingCollector {

    public static record GroupedRatings(String key, List<ForecastRating.SiteRating> ratings) {}

    public static List<GroupedRatings> ratingsByDay(ForecastRating forecastRating) {
        return groupByDay(forecastRating).entrySet().stream()
                .map(entry -> new GroupedRatings(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static List<GroupedRatings> ratingsByLocation(ForecastRating forecastRating) {
        return groupByLocation(forecastRating).entrySet().stream()
                .map(entry -> new GroupedRatings(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     *  Group ratings by day and order by score (descending).
     */
    private static Map<String, List<ForecastRating.SiteRating>> groupByDay(ForecastRating forecastRating) {
        return forecastRating.getSiteRatings().stream()
                .sorted(Comparator.comparing(ForecastRatingCollector::dayOf)
                        .thenComparing(ForecastRatingCollector::scoreOf, Comparator.reverseOrder()))
                .collect(Collectors.groupingBy(ForecastRatingCollector::formatDate, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     *  Group ratings by location and order by time (ascending).
     *  Note: The returned SiteRatings are modified from the original, so it can be used in the forecast html template.
     *  <pre>
     *      rating.location = rating.name
     *      rating.name = formatDate(rating)
     *  </pre>
     */
    private static Map<String, List<ForecastRating.SiteRating>> groupByLocation(ForecastRating forecastRating) {
        return forecastRating.getSiteRatings().stream()
                .map(r -> r.toBuilder().name(formatDate(r)).location(r.name).build())
                .sorted(Comparator.comparing(ForecastRating.SiteRating::getTime))
                .collect(Collectors.groupingBy(ForecastRating.SiteRating::getLocation, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Formats the given date as '{WeekDay}, {DayOfMonth} {NameOfMonth}'.
     * Eg. 'Söndag, 01 januari'.
     *
     * @param siteRating required; non null.
     * @return formatted date.
     */
    private static String formatDate(ForecastRating.SiteRating siteRating) {
        Locale svSE = Locale.of("sv", "SE");
        String formattedDate = String.format(svSE, "%1$tA, %1$td %1$tB",
                siteRating.time.atZone(ZoneId.of("Europe/Stockholm")));
        return StringUtils.capitalize(formattedDate);
    }

    private static Instant dayOf(ForecastRating.SiteRating siteRating) {
        return siteRating.time.atZone(ZoneId.of("Europe/Stockholm")).truncatedTo(ChronoUnit.DAYS).toInstant();
    }

    private static Double scoreOf(ForecastRating.SiteRating siteRating) {
        return siteRating.score;
    }

}
