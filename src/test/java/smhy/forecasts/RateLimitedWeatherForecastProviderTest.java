package smhy.forecasts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import smhy.locations.ForecastLocation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitedWeatherForecastProviderTest {

    @Mock
    private WeatherForecastProvider wrappedProvider;

    @Mock
    private InMemoryForecastStore forecastStore;

    private RateLimitedWeatherForecastProvider provider;

    private ForecastLocation location;

    @BeforeEach
    void setUp() {
        provider = new RateLimitedWeatherForecastProvider(wrappedProvider, forecastStore);
        location = ForecastLocation.builder()
                .id("salanda")
                .name("Sälanda")
                .latitude("58.0")
                .longitude("12.0")
                .altitude("100")
                .build();
    }

    @Test
    void getProvider_delegatesToWrappedProvider() {
        when(wrappedProvider.getProvider()).thenReturn("yr");
        assertThat(provider.getProvider()).isEqualTo("yr");
        verify(wrappedProvider).getProvider();
    }

    @Test
    void getForecastForLocation_callsWrappedProviderAndSaves() {
        WeatherForecast fresh = WeatherForecast.builder()
                .id("20260405")
                .provider("yr")
                .forecastLocation(location)
                .forecastDate(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .items(List.of())
                .build();

        when(wrappedProvider.getForecastForLocation(location)).thenReturn(Optional.of(fresh));

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(fresh);
        verify(wrappedProvider).getForecastForLocation(location);
        verify(forecastStore).save(fresh);
    }

    @Test
    void getForecastForLocation_wrappedProviderReturnsEmpty_returnsEmptyAndDoesNotSave() {
        when(wrappedProvider.getForecastForLocation(location)).thenReturn(Optional.empty());

        Optional<WeatherForecast> result = provider.getForecastForLocation(location);

        assertThat(result).isEmpty();
        verify(forecastStore, never()).save(any());
    }
}
