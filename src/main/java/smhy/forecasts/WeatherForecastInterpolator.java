package smhy.forecasts;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class WeatherForecastInterpolator {

    /**
     * Interpolates missing items in forecast by copying the previous item.
     * @param items required; non-null
     * @return List of WeatherForecastItems with interpolated items added.
     */
    public List<WeatherForecast.WeatherForecastItem> interpolate(List<WeatherForecast.WeatherForecastItem> items) {
        List<WeatherForecast.WeatherForecastItem> interpolatedItems = new ArrayList<>();
        for (int i=0; i < items.size()-1; i++) {
            WeatherForecast.WeatherForecastItem currentItem = items.get(i);
            WeatherForecast.WeatherForecastItem nextItem = items.get(i + 1);
            interpolatedItems.add(currentItem);
            int hoursBetweenItems = numberOfHoursBetweenItems(currentItem, nextItem);
            if (hoursBetweenItems > 1) {
                for (int hourOffset = 1; hourOffset < hoursBetweenItems; hourOffset++) {
                    interpolatedItems.add(currentItem.toBuilder()
                            .time(addNumberOfHoursToDate(currentItem.getTime(), hourOffset))
                            .build());
                }
            }
        }
        interpolatedItems.add(items.get(items.size() - 1));
        return interpolatedItems;
    }

    int numberOfHoursBetweenItems(WeatherForecast.WeatherForecastItem item1, WeatherForecast.WeatherForecastItem item2) {
        return Duration.between(item1.getTime(), item2.getTime()).toHoursPart();
    }

    Instant addNumberOfHoursToDate(Instant date, int numHours) {
        return date.plus(numHours, ChronoUnit.HOURS);
    }
}
