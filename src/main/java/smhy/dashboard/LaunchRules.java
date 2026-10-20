package smhy.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import smhy.conditions.ForecastCondition;
import smhy.conditions.ForecastConditionService;
import smhy.conditions.WindDirection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Launch sectors shown on the compass, taken from {@code conditions.yml}.
 * <p>
 * A condition counts for the current month when {@code monthsOfYear} is empty or contains that month.
 * Weekday limits are ignored here: the compass describes the hill, and the rating engine still
 * applies {@code daysOfWeek} when it scores a forecast.
 * <p>
 * Directions on the highest-scoring conditions are "good". Directions on weaker conditions
 * (score at least 25, and not a near-complete catch-all) are "ok". The ideal wind range is taken
 * from the highest-scoring conditions only.
 */
@Component
@RequiredArgsConstructor
public class LaunchRules {

    static final List<String> COMPASS = List.of(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW");

    /** A weak "any direction" fallback should not paint the whole compass amber. */
    private static final int CATCH_ALL_DIRECTIONS = 14;
    private static final double OK_SCORE_MIN = 25.0;

    private final ForecastConditionService forecastConditionService;

    public Dashboard.Launch forLocation(String locationId, int month) {
        List<ForecastCondition> applicable = forecastConditionService.findByLocation(locationId).stream()
                .filter(condition -> appliesInMonth(condition, month))
                .toList();
        if (applicable.isEmpty()) {
            return Dashboard.Launch.builder().good(List.of()).ok(List.of()).build();
        }

        double maxScore = applicable.stream()
                .map(ForecastCondition::getScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);

        List<ForecastCondition> top = applicable.stream()
                .filter(condition -> condition.getScore() != null && condition.getScore() >= maxScore - 0.01)
                .toList();

        Set<String> good = new LinkedHashSet<>();
        for (ForecastCondition condition : top) {
            good.addAll(directionsOf(condition));
        }

        Set<String> ok = new LinkedHashSet<>();
        for (ForecastCondition condition : applicable) {
            if (condition.getScore() == null || condition.getScore() < OK_SCORE_MIN || condition.getScore() >= maxScore - 0.01) {
                continue;
            }
            List<String> directions = directionsOf(condition);
            if (directions.size() >= CATCH_ALL_DIRECTIONS) {
                continue;
            }
            for (String direction : directions) {
                if (!good.contains(direction)) {
                    ok.add(direction);
                }
            }
        }

        return Dashboard.Launch.builder()
                .good(sorted(good))
                .ok(sorted(ok))
                .windMin(min(top, ForecastCondition::getWindSpeedMin))
                .windMax(max(top, ForecastCondition::getWindSpeedMax))
                .gustMax(max(top, ForecastCondition::getWindGustMax))
                .build();
    }

    private static boolean appliesInMonth(ForecastCondition condition, int month) {
        List<Integer> months = condition.getMonthsOfYear();
        return months == null || months.isEmpty() || months.contains(month);
    }

    private static List<String> directionsOf(ForecastCondition condition) {
        if (condition.getWindDirections() == null || condition.getWindDirections().isEmpty()) {
            return List.of();
        }
        List<String> directions = new ArrayList<>();
        for (WindDirection direction : condition.getWindDirections()) {
            if (direction != null) {
                directions.add(direction.name());
            }
        }
        return directions;
    }

    private static List<String> sorted(Set<String> directions) {
        return directions.stream()
                .sorted(Comparator.comparingInt(COMPASS::indexOf))
                .toList();
    }

    private static Double min(List<ForecastCondition> conditions, java.util.function.Function<ForecastCondition, Double> getter) {
        return conditions.stream().map(getter).filter(Objects::nonNull).min(Double::compare).orElse(null);
    }

    private static Double max(List<ForecastCondition> conditions, java.util.function.Function<ForecastCondition, Double> getter) {
        return conditions.stream().map(getter).filter(Objects::nonNull).max(Double::compare).orElse(null);
    }
}
