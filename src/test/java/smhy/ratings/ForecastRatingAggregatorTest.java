package smhy.ratings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForecastRatingAggregatorTest {

    private ForecastRatingAggregator forecastRatingAggregator;

    @BeforeEach
    void setUp() {
        forecastRatingAggregator = new ForecastRatingAggregator();
    }

    @Test
    void testAggregateRatingsOnEmptyListShouldReturnEmptyList() {
        Collection<ForecastRating.SiteRating> siteRatings = forecastRatingAggregator.aggregateRatings(List.of());
        assertTrue(siteRatings.isEmpty());
    }

    @Test
    void testAggregateRatingsOnSingleItemWithZeroScoreAndHoursMinShouldReturnEmptyList() {
        Collection<ForecastRating.SiteRating> siteRatings = forecastRatingAggregator.aggregateRatings(List.of(
                builder().build()
        ));
        assertTrue(siteRatings.isEmpty());
    }

    @Test
    void testAggregateRatingsOnSingleItemWithZeroScoreShouldReturnEmptyList() {
        Collection<ForecastRating.SiteRating> siteRatings = forecastRatingAggregator.aggregateRatings(List.of(
                builder()
                        .score(0.0)
                        .hoursMin(24.0)
                        .build()
        ));
        assertTrue(siteRatings.isEmpty());
    }

    @Test
    void testAggregateRatingsOnSingleItemWithZeroHoursMinShouldReturnEmptyList() {
        Collection<ForecastRating.SiteRating> siteRatings = forecastRatingAggregator.aggregateRatings(
                List.of(builder()
                        .score(99.0)
                        .hoursMin(0.0)
                        .build()
        ));
        assertTrue(siteRatings.isEmpty());
    }

    @Test
    void testAggregateRatingsOnSingleItemWithScoreAndHoursMinGreaterThanLimitShouldReturnSingleRating() {
        List<ForecastRating.SiteRating> inputRatings = List.of(builder()
                .score(99.0)
                .hoursMin(24.0)
                .build());
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(1, actualRatings.size());
        assertEquals(inputRatings.get(0), actualRatings.stream().toList().get(0));
    }

    @Test
    void testAggregateRatingsOnMultipleItemsWithScoreGreaterThanZeroAndHoursMinLessThanLimitShouldReturnEmptyList() {
        List<ForecastRating.SiteRating> inputRatings = List.of(
                builder().score(99.0).timeOfDayMin(8).timeOfDayMax(8).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(9).timeOfDayMax(9).hoursMin(1.0).build()
        );
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(0, actualRatings.size());
    }

    @Test
    void testAggregateRatingsOnMultipleItemsWithScoreGreaterThanZeroAndHoursMinGreaterThanLimitShouldReturnEmptyList() {
        List<ForecastRating.SiteRating> inputRatings = List.of(
                builder().score(99.0).timeOfDayMin(7).timeOfDayMax(7).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(8).timeOfDayMax(8).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(9).timeOfDayMax(9).hoursMin(1.0).build()
        );
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(1, actualRatings.size());
        ForecastRating.SiteRating expected = builder().score(99.0).timeOfDayMin(7).timeOfDayMax(9).hoursMin(3.0).build();
        ForecastRating.SiteRating actual = actualRatings.stream().toList().get(0);
        assertEquals(expected, actual);
    }

    @Test
    void testAggregateRatingsShouldMergeMultipleItemsIntoSingleItemWhenScoreIsGreaterThanZeroOnConsecutiveHours() {
        List<ForecastRating.SiteRating> inputRatings = List.of(
                builder().score(99.0).timeOfDayMin(1).timeOfDayMax(1).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(2).timeOfDayMax(2).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(3).timeOfDayMax(3).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(4).timeOfDayMax(4).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(5).timeOfDayMax(5).hoursMin(1.0).build(),
                builder().score(99.0).timeOfDayMin(6).timeOfDayMax(6).hoursMin(1.0).build()
        );
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(1, actualRatings.size());
        ForecastRating.SiteRating expected = builder().score(99.0).timeOfDayMin(1).timeOfDayMax(6).hoursMin(6.0).build();
        ForecastRating.SiteRating actual = actualRatings.stream().toList().get(0);
        assertEquals(expected, actual);
    }

    @Test
    void testAggregateRatingsShouldNotMergeMultipleItemsIntoSingleItemHoursAreNotConsecutive() {
        List<ForecastRating.SiteRating> inputRatings = List.of(
                builder().score(1.0).timeOfDayMin(1).timeOfDayMax(1).hoursMin(1.0).build(),
                builder().score(1.0).timeOfDayMin(2).timeOfDayMax(2).hoursMin(1.0).build(),
                builder().score(1.0).timeOfDayMin(3).timeOfDayMax(3).hoursMin(1.0).build(),
                builder().score(0.0).timeOfDayMin(4).timeOfDayMax(4).hoursMin(1.0).build(),
                builder().score(2.0).timeOfDayMin(5).timeOfDayMax(5).hoursMin(1.0).build(),
                builder().score(2.0).timeOfDayMin(6).timeOfDayMax(6).hoursMin(1.0).build(),
                builder().score(2.0).timeOfDayMin(7).timeOfDayMax(7).hoursMin(1.0).build()
        );
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(2, actualRatings.size());
        ForecastRating.SiteRating expected1 = builder().score(1.0).timeOfDayMin(1).timeOfDayMax(3).hoursMin(3.0).build();
        ForecastRating.SiteRating expected2 = builder().score(2.0).timeOfDayMin(5).timeOfDayMax(7).hoursMin(3.0).build();
        ForecastRating.SiteRating actual1 = actualRatings.stream().toList().get(0);
        ForecastRating.SiteRating actual2 = actualRatings.stream().toList().get(1);
        assertEquals(expected1, actual1);
        assertEquals(expected2, actual2);
    }

    @Test
    void testAggregateRatingsShouldNotMergeMultipleItemsIntoSingleItemWhenDateAreNotTheSame() {
        List<ForecastRating.SiteRating> inputRatings = List.of(
                builder().score(1.0).timeOfDayMin(1).timeOfDayMax(1).hoursMin(1.0).time(Instant.parse("2023-01-01T01:00:00Z")).build(),
                builder().score(1.0).timeOfDayMin(2).timeOfDayMax(2).hoursMin(1.0).time(Instant.parse("2023-01-01T02:00:00Z")).build(),
                builder().score(1.0).timeOfDayMin(3).timeOfDayMax(3).hoursMin(1.0).time(Instant.parse("2023-01-01T03:00:00Z")).build(),
                // Note! There must always be at least on zero score item between days or else the algorithm breaks down.
                // This is usually the case, but may fail if timeOfDayMin=0 and timeOfDayMax=23 or the sun never sets at given location.
                builder().score(0.0).timeOfDayMin(4).timeOfDayMax(4).hoursMin(1.0).time(Instant.parse("2023-01-01T04:00:00Z")).build(),
                builder().score(2.0).timeOfDayMin(4).timeOfDayMax(4).hoursMin(1.0).time(Instant.parse("2023-01-02T04:00:00Z")).build(),
                builder().score(2.0).timeOfDayMin(5).timeOfDayMax(5).hoursMin(1.0).time(Instant.parse("2023-01-02T05:00:00Z")).build(),
                builder().score(2.0).timeOfDayMin(6).timeOfDayMax(6).hoursMin(1.0).time(Instant.parse("2023-01-02T06:00:00Z")).build()
        );
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(2, actualRatings.size());
        ForecastRating.SiteRating expected1 = builder().score(1.0).timeOfDayMin(1).timeOfDayMax(3).hoursMin(3.0).time(Instant.parse("2023-01-01T01:00:00Z")).build();
        ForecastRating.SiteRating expected2 = builder().score(2.0).timeOfDayMin(4).timeOfDayMax(6).hoursMin(3.0).time(Instant.parse("2023-01-02T04:00:00Z")).build();
        ForecastRating.SiteRating actual1 = actualRatings.stream().toList().get(0);
        ForecastRating.SiteRating actual2 = actualRatings.stream().toList().get(1);
        assertEquals(expected1, actual1);
        assertEquals(expected2, actual2);
    }

    @Test
    void testAggregateRatingsShouldCalculateAverageScoreWhenMergingMultipleItemsIntoSingleItem() {
        List<ForecastRating.SiteRating> inputRatings = List.of(
                builder().score(1.0).timeOfDayMin(1).timeOfDayMax(1).hoursMin(1.0).build(),
                builder().score(2.0).timeOfDayMin(2).timeOfDayMax(2).hoursMin(1.0).build(),
                builder().score(3.0).timeOfDayMin(3).timeOfDayMax(3).hoursMin(1.0).build()
        );
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(1, actualRatings.size());
        ForecastRating.SiteRating expected = builder().score(2.0).timeOfDayMin(1).timeOfDayMax(3).hoursMin(3.0).build();
        ForecastRating.SiteRating actual = actualRatings.stream().toList().get(0);
        assertEquals(expected, actual);
    }

    @Test
    void testAggregateRatingsShouldCalculateMinMaxValues() {
        List<ForecastRating.SiteRating> inputRatings = List.of(
                builder().score(1.0).timeOfDayMin(1).timeOfDayMax(2).hoursMin(1.0)
                        .windDirectionMin(1.0)
                        .windDirectionMax(2.0)
                        .windSpeedMin(1.0)
                        .windSpeedMax(2.0)
                        .windGustMax(1.0)
                        .airTemperatureMin(1.0)
                        .airTemperatureMax(2.0)
                        .airPressureMin(1.0)
                        .airPressureMax(2.0)
                        .relativeHumidityMin(1.0)
                        .relativeHumidityMax(2.0)
                        .probabilityPrecipitationMin(1.0)
                        .probabilityPrecipitationMax(2.0)
                        .precipitationAmountMin(1.0)
                        .precipitationAmountMax(2.0)
                        .dewPointTemperatureMin(1.0)
                        .dewPointTemperatureMax(2.0)
                        .build(),
                builder().score(1.0).timeOfDayMin(2).timeOfDayMax(1).hoursMin(1.0)
                        .windDirectionMin(1.0)
                        .windDirectionMax(9.0)
                        .windSpeedMin(1.0)
                        .windSpeedMax(9.0)
                        .windGustMax(9.0)
                        .airTemperatureMin(1.0)
                        .airTemperatureMax(9.0)
                        .airPressureMin(1.0)
                        .airPressureMax(9.0)
                        .relativeHumidityMin(1.0)
                        .relativeHumidityMax(9.0)
                        .probabilityPrecipitationMin(1.0)
                        .probabilityPrecipitationMax(9.0)
                        .precipitationAmountMin(1.0)
                        .precipitationAmountMax(9.0)
                        .dewPointTemperatureMin(1.0)
                        .dewPointTemperatureMax(9.0)
                        .build(),
                builder().score(1.0).timeOfDayMin(3).timeOfDayMax(3).hoursMin(1.0)
                        .windDirectionMin(0.0)
                        .windDirectionMax(2.0)
                        .windSpeedMin(0.0)
                        .windSpeedMax(2.0)
                        .windGustMax(0.0)
                        .airTemperatureMin(0.0)
                        .airTemperatureMax(2.0)
                        .airPressureMin(0.0)
                        .airPressureMax(2.0)
                        .relativeHumidityMin(0.0)
                        .relativeHumidityMax(2.0)
                        .probabilityPrecipitationMin(0.0)
                        .probabilityPrecipitationMax(2.0)
                        .precipitationAmountMin(0.0)
                        .precipitationAmountMax(2.0)
                        .dewPointTemperatureMin(0.0)
                        .dewPointTemperatureMax(2.0)
                        .build(),
                builder().score(1.0).timeOfDayMin(4).timeOfDayMax(4).hoursMin(1.0)
                        .windDirectionMin(1.0)
                        .windDirectionMax(2.0)
                        .windSpeedMin(1.0)
                        .windSpeedMax(2.0)
                        .windGustMax(1.0)
                        .airTemperatureMin(1.0)
                        .airTemperatureMax(2.0)
                        .airPressureMin(1.0)
                        .airPressureMax(2.0)
                        .relativeHumidityMin(1.0)
                        .relativeHumidityMax(2.0)
                        .probabilityPrecipitationMin(1.0)
                        .probabilityPrecipitationMax(2.0)
                        .precipitationAmountMin(1.0)
                        .precipitationAmountMax(2.0)
                        .dewPointTemperatureMin(1.0)
                        .dewPointTemperatureMax(2.0)
                        .build()
        );
        Collection<ForecastRating.SiteRating> actualRatings = forecastRatingAggregator.aggregateRatings(inputRatings);
        assertEquals(1, actualRatings.size());
        ForecastRating.SiteRating expected = builder().score(1.0).timeOfDayMin(1).timeOfDayMax(4).hoursMin(4.0)
                .windDirectionMin(0.0)
                .windDirectionMax(9.0)
                .windSpeedMin(0.0)
                .windSpeedMax(9.0)
                .windGustMax(9.0)
                .airTemperatureMin(0.0)
                .airTemperatureMax(9.0)
                .airPressureMin(0.0)
                .airPressureMax(9.0)
                .relativeHumidityMin(0.0)
                .relativeHumidityMax(9.0)
                .probabilityPrecipitationMin(0.0)
                .probabilityPrecipitationMax(9.0)
                .precipitationAmountMin(0.0)
                .precipitationAmountMax(9.0)
                .dewPointTemperatureMin(0.0)
                .dewPointTemperatureMax(9.0)
                .build();
        ForecastRating.SiteRating actual = actualRatings.stream().toList().get(0);
        assertEquals(expected, actual);
    }

    private static ForecastRating.SiteRating.SiteRatingBuilder builder() {
        return ForecastRating.SiteRating.builder()
                .time(Instant.parse("2023-01-01T00:00:00Z"))
                .name("alleberg")
                .location("alleberg")
                .score(0.0)
                .timeOfDayMin(0)
                .timeOfDayMax(0)
                .airPressureMax(0.0)
                .airPressureMin(0.0)
                .airTemperatureMin(0.0)
                .airTemperatureMax(0.0)
                .dewPointTemperatureMin(0.0)
                .dewPointTemperatureMax(0.0)
                .precipitationAmountMin(0.0)
                .precipitationAmountMax(0.0)
                .relativeHumidityMin(0.0)
                .relativeHumidityMax(0.0)
                .windDirectionMin(0.0)
                .windDirectionMax(0.0)
                .windGustMax(0.0)
                .windSpeedMin(0.0)
                .windSpeedMax(0.0)
                .probabilityPrecipitationMin(0.0)
                .probabilityPrecipitationMax(0.0)
                .hoursMin(0.0);
    }
}