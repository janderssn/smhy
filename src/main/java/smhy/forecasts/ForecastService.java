package smhy.forecasts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import smhy.locations.ForecastLocationService;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastService {

    private final ForecastLocationService forecastLocationService;
    private final WeatherForecastProvider forecastProvider;

    public void runAll(List<String> locationIds) {
        forecastLocationService.findAll().stream()
                .filter(l -> isEmpty(locationIds) || locationIds.contains(l.getId()) )
                .forEach(forecastProvider::getForecastForLocation);
    }

}
