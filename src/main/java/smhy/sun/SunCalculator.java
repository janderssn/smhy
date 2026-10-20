package smhy.sun;

import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Sunrise and sunset for a site on a local calendar day.
 * Times are in Europe/Stockholm, which is the zone the ratings use.
 */
@Component
public class SunCalculator {

    public static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");

    public record Span(LocalTime rise, LocalTime set) {}

    public Optional<Span> on(double latitude, double longitude, LocalDate date) {
        ZonedDateTime noon = date.atTime(12, 0).atZone(ZONE);
        SunTimes sun = SunTimes.compute()
                .on(noon)
                .timezone(ZONE)
                .at(latitude, longitude)
                .execute();
        if (sun.getRise() == null || sun.getSet() == null) {
            return Optional.empty();
        }
        return Optional.of(new Span(LocalTime.from(sun.getRise()), LocalTime.from(sun.getSet())));
    }
}
