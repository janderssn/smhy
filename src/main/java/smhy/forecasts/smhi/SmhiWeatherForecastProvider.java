package smhy.forecasts.smhi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import smhy.forecasts.AbstractWeatherForecastProvider;
import smhy.locations.ForecastLocation;
import smhy.forecasts.WeatherForecast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service("smhi")
@ConditionalOnProperty(name = "smhy.provider", havingValue = "smhi")
public class SmhiWeatherForecastProvider extends AbstractWeatherForecastProvider {

    private final RestTemplate restTemplate;
    private final SmhiWeatherForecastConverter converter;

    @Value("${smhy.forecasts.smhi.url:}")
    private String url;

    @Autowired
    public SmhiWeatherForecastProvider(
            SmhiWeatherForecastConverter converter,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${smhy.forecasts.smhi.useragent:}") String userAgent) {
        this.converter = converter;
        this.restTemplate = restTemplateBuilder
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    @PostConstruct
    public void init() {
        if (hasText(url)) {
            log.info("SMHI forecast provider enabled");
        } else {
            log.warn("SMHI forecast provider disabled");
            log.warn("Configure smhy.forecasts.smhi.url in application.properties to enable SMHI forecast provider");
        }
    }

    @Override
    public String getProvider() {
        return "smhi";
    }

    @Override
    public Optional<WeatherForecast> getForecastForLocation(ForecastLocation location) {
        if (!hasText(url)) {
            log.warn("Configure smhy.forecasts.smhi.url in application.properties to enable SMHI forecast provider");
            return Optional.empty();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .build(location.getLongitude(), location.getLatitude());
        ResponseEntity<ObjectNode> responseEntity = restTemplate.getForEntity(uri, ObjectNode.class);
        ObjectNode body = responseEntity.getBody();
        if (body != null) {
            WeatherForecast weatherForecast = converter.convert(body);
            weatherForecast.setProvider(getProvider());
            weatherForecast.setExpiresAt(expiresAsDate(responseEntity.getHeaders().getExpires()));
            weatherForecast.setForecastLocation(location);
            setSunriseSunsetTimes(weatherForecast);
            return Optional.of(weatherForecast);
        }
        return Optional.empty();
    }

}
