package smhy.forecasts;

import com.google.common.util.concurrent.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import smhy.locations.ForecastLocation;

import java.util.Optional;

/** @noinspection UnstableApiUsage */
@Slf4j
@Service
@Primary
@AllArgsConstructor
public class RateLimitedWeatherForecastProvider implements WeatherForecastProvider {

    /**
     * Maximum number of requests per second made to YR API.
     * The Terms Of Service restrict clients to at most 20 requests per second total.
     */
    private static final double REQUESTS_PER_SECOND = 1.0;
    private static final RateLimiter rateLimiter = RateLimiter.create(REQUESTS_PER_SECOND);

    private final RestApiWeatherForecastProvider weatherForecastProvider;
    private final InMemoryForecastStore forecastStore;

    public String getProvider() {
        return weatherForecastProvider.getProvider();
    }

    @Override
    public Optional<WeatherForecast> getForecastForLocation(ForecastLocation location) {
        log.info("Requesting forecast for location {}", location.getId());
        rateLimiter.acquire();
        Optional<WeatherForecast> forecast = weatherForecastProvider.getForecastForLocation(location);
        forecast.ifPresent(forecastStore::save);
        return forecast;
    }
}
