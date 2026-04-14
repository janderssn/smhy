package smhy.forecasts;

import org.shredzone.commons.suncalc.SunTimes;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public abstract class AbstractWeatherForecastProvider implements WeatherForecastProvider {

    protected void setSunriseSunsetTimes(WeatherForecast forecast) {

        double lat = Double.valueOf(forecast.getForecastLocation().getLatitude());
        double lng = Double.valueOf(forecast.getForecastLocation().getLongitude());

        SunTimes parameters = SunTimes.compute()
                .on(forecast.getForecastDate().truncatedTo(ChronoUnit.DAYS))
                .timezone("Europe/Stockholm")
                .at(lat, lng)
                .oneDay()
                .execute();

        forecast.setSunrise(LocalTime.from(parameters.getRise()));
        forecast.setSunset(LocalTime.from(parameters.getSet()));
    }

}
