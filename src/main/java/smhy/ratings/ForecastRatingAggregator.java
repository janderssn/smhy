package smhy.ratings;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregates ratings into spans of consecutive hours having a score greater than zero for at least 3 hours.
 * This is used when displaying summary of ratings in ui. Instead of showing the score for every hour of the day,
 * the average score is shown for the range of hours having a score greater than 0.
 *
 * <p>For example:
 * <pre>
 *   | time             | score | wd  | ws  | ...
 *   | 2022-07-01 09:00 |     0 | 200 | 1.0 | ...
 *   | 2022-07-01 10:00 |    50 | 202 | 5.1 | ...
 *   | 2022-07-01 11:00 |   100 | 205 | 5.4 | ...
 *   | 2022-07-01 12:00 |    50 | 204 | 5.2 | ...
 *   | 2022-07-01 13:00 |     0 | 207 | 7.0 | ...
 * </pre>
 * <p>Results in the following aggregation:
 * <pre>
 *   | time             | score | wdMin | wdMax | wsMin | wsMax | ...
 *   | 2022-07-01 10:00 |    66 |   202 |   205 |   5.1 |   5.4 | ...
 * </pre>
 *
 * Note! There must always be at least one zero score item between two days or else the algorithm breaks down.
 * This is usually the case, but may fail if timeOfDayMin=0 and timeOfDayMax=23 or if the sun never sets at the given
 * location.
 */
@Component
public class ForecastRatingAggregator {

    /**
     * Aggregate ratings into spans of consecutive hours having a score greater than zero.
     * @param ratings required; non-null
     * @return Collection of SiteRatings with a score greater than zero.
     */
    public Collection<ForecastRating.SiteRating> aggregateRatings(List<ForecastRating.SiteRating> ratings) {
        
        for (int i = 1; i < ratings.size(); i++) {
            ForecastRating.SiteRating next = ratings.get(i);
            ForecastRating.SiteRating prev = ratings.get(i-1);
            if (prev.score > 0 && next.score > 0) {
                next.timeOfDayMin = prev.timeOfDayMin;
            }
        }

        for (int i = ratings.size() - 1; i > 0; i--) {
            ForecastRating.SiteRating next = ratings.get(i);
            ForecastRating.SiteRating prev = ratings.get(i-1);
            if (prev.score > 0 && next.score > 0) {
                prev.score = prev.score + next.score;
                prev.hoursMin = next.hoursMin + 1;
                prev.timeOfDayMax = next.timeOfDayMax;
                prev.windDirectionMax = Math.max(prev.windDirectionMax, next.windDirectionMax);
                prev.windSpeedMax = Math.max(prev.windSpeedMax, next.windSpeedMax);
                prev.windGustMax = Math.max(prev.windGustMax, next.windGustMax);
                prev.windDirectionMin = Math.min(prev.windDirectionMin, next.windDirectionMin);
                prev.windSpeedMin = Math.min(prev.windSpeedMin, next.windSpeedMin);
                prev.airTemperatureMin = Math.min(prev.airTemperatureMin, next.airTemperatureMin);
                prev.airTemperatureMax = Math.max(prev.airTemperatureMax, next.airTemperatureMax);
                prev.airPressureMin = Math.min(prev.airPressureMin, next.airPressureMin);
                prev.airPressureMax = Math.max(prev.airPressureMax, next.airPressureMax);
                prev.relativeHumidityMin = Math.min(prev.relativeHumidityMin, next.relativeHumidityMin);
                prev.relativeHumidityMax = Math.max(prev.relativeHumidityMax, next.relativeHumidityMax);
                prev.precipitationAmountMin = Math.min(prev.precipitationAmountMin, next.precipitationAmountMin);
                prev.precipitationAmountMax = Math.max(prev.precipitationAmountMax, next.precipitationAmountMax);
                prev.probabilityPrecipitationMin = Math.min(prev.probabilityPrecipitationMin, next.probabilityPrecipitationMin);
                prev.probabilityPrecipitationMax = Math.max(prev.probabilityPrecipitationMax, next.probabilityPrecipitationMax);
                prev.dewPointTemperatureMin = Math.min(prev.dewPointTemperatureMin, next.dewPointTemperatureMin);
                prev.dewPointTemperatureMax = Math.max(prev.dewPointTemperatureMax, next.dewPointTemperatureMax);
            }
        }

        return ratings.stream()
                .filter(r -> r.score > 0 && r.hoursMin >= 3)
                .peek(r -> r.score = r.score / r.hoursMin )
                .collect(Collectors.toMap(this::groupByMinTimeOfDay, p -> p, (p, q) -> p))
                .values();
    }

    /**
     * Returns key for grouping SiteRatings by date and timeOfDayMin.
     * This will group ratings into consecutive hours with score greater than zero.
     * @param rating required; non-null
     * @return Instant of day(time) + timeOfDayMin
     */
    private Instant groupByMinTimeOfDay(ForecastRating.SiteRating rating) {
        return rating.time.truncatedTo(ChronoUnit.DAYS)
                .plus(rating.timeOfDayMin, ChronoUnit.HOURS);
    }
}