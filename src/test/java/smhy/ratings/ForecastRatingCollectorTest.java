package smhy.ratings;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForecastRatingCollectorTest {

    ForecastRating forecastRating = ForecastRating.builder()
            .siteRatings(List.of(
                    ForecastRating.SiteRating.builder()
                            .location("alleberg")
                            .name("Ålleberg")
                            .time(LocalDateTime.parse("2022-07-01T10:00:00").toInstant(ZoneOffset.UTC))
                            .score(9.0)
                            .build(),
                    ForecastRating.SiteRating.builder()
                            .location("alleberg")
                            .name("Ålleberg")
                            .time(LocalDateTime.parse("2022-07-02T10:00:00").toInstant(ZoneOffset.UTC))
                            .score(7.0)
                            .build(),
                    ForecastRating.SiteRating.builder()
                            .location("salanda")
                            .name("Salanda")
                            .time(LocalDateTime.parse("2022-07-01T10:00:00").toInstant(ZoneOffset.UTC))
                            .score(5.0)
                            .build(),
                    ForecastRating.SiteRating.builder()
                            .location("brahehus")
                            .name("Brahehus")
                            .time(LocalDateTime.parse("2022-07-01T10:00:00").toInstant(ZoneOffset.UTC))
                            .score(2.0)
                            .build()
            ))
            .build();

    @Test
    void testRatingsByDay() {
        List<ForecastRatingCollector.GroupedRatings> groupedRatings = ForecastRatingCollector.ratingsByDay(forecastRating);
        assertEquals(2, groupedRatings.size());
        assertEquals(List.of("Fredag, 01 juli","Lördag, 02 juli"), groupedRatings.stream().map(g -> g.key()).toList());
        assertEquals(3, groupedRatings.get(0).ratings().size());
        assertEquals(1, groupedRatings.get(1).ratings().size());
    }

    @Test
    void testRatingsByLocation() {
        List<ForecastRatingCollector.GroupedRatings> groupedRatings = ForecastRatingCollector.ratingsByLocation(forecastRating);
        assertEquals(3, groupedRatings.size());
        assertEquals(List.of("Ålleberg","Salanda","Brahehus"), groupedRatings.stream().map(g -> g.key()).toList());
        assertEquals(2, groupedRatings.get(0).ratings().size());
        assertEquals(1, groupedRatings.get(1).ratings().size());
        assertEquals(1, groupedRatings.get(2).ratings().size());

    }
}