package smhy.forecasts;

import smhy.locations.ForecastLocation;

import java.util.Optional;

public interface WeatherForecastProvider {

    Optional<WeatherForecast> getForecastForLocation(ForecastLocation location);
    String getProvider();
}
