package smhy.forecasts.smhi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import smhy.forecasts.WeatherForecast;
import smhy.locations.ForecastLocation;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmhiWeatherForecastProviderTest {

    @Mock
    private SmhiWeatherForecastConverter converter;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    private SmhiWeatherForecastProvider provider;

    private ForecastLocation location;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.defaultHeader(anyString(), anyString())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        provider = new SmhiWeatherForecastProvider(converter, restTemplateBuilder, "test-agent");

        location = ForecastLocation.builder()
                .id("salanda")
                .name("Sälanda")
                .latitude("58.0")
                .longitude("12.0")
                .altitude("100")
                .build();
    }

    @Test
    void getProvider_returnsSmhi() {
        assertThat(provider.getProvider()).isEqualTo("smhi");
    }

    @Test
    void getForecastForLocation_whenUrlEmpty_returnsEmpty() {
        ReflectionTestUtils.setField(provider, "url", "");

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);

        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
        verifyNoInteractions(converter);
    }

    @Test
    void getForecastForLocation_whenUrlBlank_returnsEmpty() {
        ReflectionTestUtils.setField(provider, "url", "   ");

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);

        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getForecastForLocation_whenBodyNull_returnsEmpty() {
        ReflectionTestUtils.setField(provider, "url",
                "http://example.com/api/{longitude}/{latitude}/data.json");

        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<ObjectNode> responseEntity = ResponseEntity.ok().headers(headers).body(null);

        when(restTemplate.getForEntity(any(URI.class), eq(ObjectNode.class))).thenReturn(responseEntity);

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);

        assertThat(result).isEmpty();
        verifyNoInteractions(converter);
    }

    @Test
    void getForecastForLocation_whenUrlSet_callsRestTemplateAndReturnsConvertedForecast() throws Exception {
        ReflectionTestUtils.setField(provider, "url",
                "http://example.com/api/{longitude}/{latitude}/data.json");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode body = objectMapper.createObjectNode();

        HttpHeaders headers = new HttpHeaders();
        // getExpires() returns -1 when not set, triggering Instant.now() path
        ResponseEntity<ObjectNode> responseEntity = ResponseEntity.ok().headers(headers).body(body);

        when(restTemplate.getForEntity(any(URI.class), eq(ObjectNode.class))).thenReturn(responseEntity);

        WeatherForecast convertedForecast = WeatherForecast.builder()
                .forecastDate(Instant.now())
                .forecastLocation(location)
                .items(List.of())
                .build();
        when(converter.convert(body)).thenReturn(convertedForecast);

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);

        assertThat(result).isPresent();
        WeatherForecast forecast = result.get();
        assertThat(forecast.getProvider()).isEqualTo("smhi");
        assertThat(forecast.getForecastLocation()).isEqualTo(location);
        assertThat(forecast.getExpiresAt()).isNotNull();
        assertThat(forecast.getSunrise()).isNotNull();
        assertThat(forecast.getSunset()).isNotNull();

        verify(restTemplate).getForEntity(any(URI.class), eq(ObjectNode.class));
        verify(converter).convert(body);
    }

    // -----------------------------------------------------------------------
    // init()
    // -----------------------------------------------------------------------

    @Test
    void init_whenUrlIsSet_doesNotThrow() {
        ReflectionTestUtils.setField(provider, "url", "http://opendata-download-metfcst.smhi.se/api/forecast");

        provider.init();
    }

    @Test
    void init_whenUrlIsBlank_doesNotThrow() {
        ReflectionTestUtils.setField(provider, "url", "");

        provider.init();
    }

    // -----------------------------------------------------------------------
    // expiresAsDate — negative/zero Expires header falls back to Instant.now()
    // -----------------------------------------------------------------------

    @Test
    void getForecastForLocation_expiresAtIsApproximatelyNowWhenHeaderIsNegative() throws Exception {
        ReflectionTestUtils.setField(provider, "url",
                "http://example.com/api/{longitude}/{latitude}/data.json");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode body = objectMapper.createObjectNode();

        // No Expires header set → getExpires() returns -1
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<ObjectNode> responseEntity = ResponseEntity.ok().headers(headers).body(body);

        when(restTemplate.getForEntity(any(URI.class), eq(ObjectNode.class))).thenReturn(responseEntity);

        Instant before = Instant.now();
        WeatherForecast convertedForecast = WeatherForecast.builder()
                .forecastDate(Instant.now())
                .forecastLocation(location)
                .items(List.of())
                .build();
        when(converter.convert(body)).thenReturn(convertedForecast);

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);
        Instant after = Instant.now();

        assertThat(result).isPresent();
        Instant expiresAt = result.get().getExpiresAt();
        assertThat(expiresAt).isBetween(before, after);
    }

    @Test
    void getForecastForLocation_setsExpiresAtFromHeaderWhenPresent() throws Exception {
        ReflectionTestUtils.setField(provider, "url",
                "http://example.com/api/{longitude}/{latitude}/data.json");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode body = objectMapper.createObjectNode();

        long futureMillis = Instant.now().plusSeconds(3600).toEpochMilli();
        HttpHeaders headers = new HttpHeaders();
        headers.setExpires(futureMillis);
        ResponseEntity<ObjectNode> responseEntity = ResponseEntity.ok().headers(headers).body(body);

        when(restTemplate.getForEntity(any(URI.class), eq(ObjectNode.class))).thenReturn(responseEntity);

        WeatherForecast convertedForecast = WeatherForecast.builder()
                .forecastDate(Instant.now())
                .forecastLocation(location)
                .items(List.of())
                .build();
        when(converter.convert(body)).thenReturn(convertedForecast);

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);

        assertThat(result).isPresent();
        // HTTP Expires header has second-level precision, so compare truncated to seconds
        assertThat(result.get().getExpiresAt())
                .isEqualTo(Instant.ofEpochMilli(futureMillis).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }
}
