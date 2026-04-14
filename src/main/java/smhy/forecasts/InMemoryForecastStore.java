package smhy.forecasts;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryForecastStore {

    private final Map<String, WeatherForecast> store = new ConcurrentHashMap<>();

    public void save(WeatherForecast forecast) {
        String key = forecast.getForecastLocation().getId() + ":" + forecast.getProvider();
        store.put(key, forecast);
    }

    public Optional<WeatherForecast> findLatestByLocation(String location, String provider) {
        return Optional.ofNullable(store.get(location + ":" + provider));
    }
}
