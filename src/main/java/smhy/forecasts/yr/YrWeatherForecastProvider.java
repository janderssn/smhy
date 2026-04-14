package smhy.forecasts.yr;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import smhy.forecasts.AbstractWeatherForecastProvider;
import smhy.forecasts.RestApiWeatherForecastProvider;
import smhy.locations.ForecastLocation;
import smhy.forecasts.WeatherForecast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service("yr")
@ConditionalOnProperty(name = "smhy.provider", havingValue = "yr", matchIfMissing = true)
public class YrWeatherForecastProvider
        extends AbstractWeatherForecastProvider
        implements RestApiWeatherForecastProvider {

    private final RestTemplate restTemplate;
    private final YrWeatherForecastConverter converter;

    @Value("${smhy.forecasts.yr.url:}")
    private String url;

    @Autowired
    public YrWeatherForecastProvider(
            YrWeatherForecastConverter converter,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${smhy.forecasts.yr.useragent:}") String userAgent) {
        this.converter = converter;
        this.restTemplate = restTemplateBuilder
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    @PostConstruct
    public void init() {
        if (hasText(url)) {
            log.info("Yr forecast provider enabled");
        } else {
            log.warn("Yr forecast provider disabled");
            log.warn("Configure smhy.forecasts.yr.url in application.properties to enable Yr forecast provider");
        }
    }

    @Override
    public String getProvider() {
        return "yr";
    }

    @Override
    public Optional<WeatherForecast> getForecastForLocation(ForecastLocation location) {
        if (!hasText(url)) {
            log.warn("Configure smhy.forecasts.yr.url in application.properties to enable Yr forecast provider");
            return Optional.empty();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .build(location.getAltitude(), location.getLatitude(), location.getLongitude());
        ResponseEntity<ObjectNode> responseEntity = restTemplate.getForEntity(uri, ObjectNode.class);
        ObjectNode body = responseEntity.getBody();
        WeatherForecast weatherForecast = converter.convert(body);
        weatherForecast.setProvider(getProvider());
        weatherForecast.setExpiresAt(expiresAsDate(responseEntity.getHeaders().getExpires()));
        weatherForecast.setForecastLocation(location);
        setSunriseSunsetTimes(weatherForecast);
        return Optional.of(weatherForecast);
    }

    private Instant expiresAsDate(long value) {
        if (value > 0) {
            return Instant.ofEpochMilli(value);
        }
        return Instant.now();
    }
}
