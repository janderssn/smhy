package smhy.forecasts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import smhy.locations.ForecastLocation;
import smhy.locations.ForecastLocationService;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private ForecastLocationService forecastLocationService;

    @Mock
    private WeatherForecastProvider forecastProvider;

    @InjectMocks
    private ForecastService forecastService;

    private ForecastLocation location(String id) {
        return ForecastLocation.builder().id(id).name(id).build();
    }

    @Test
    void runAll_emptyLocationIds_processesAllLocations() {
        ForecastLocation salanda = location("salanda");
        ForecastLocation alleberg = location("alleberg");
        when(forecastLocationService.findAll()).thenReturn(List.of(salanda, alleberg));

        forecastService.runAll(List.of());

        verify(forecastProvider).getForecastForLocation(salanda);
        verify(forecastProvider).getForecastForLocation(alleberg);
    }

    @Test
    void runAll_filteredByLocationId_onlyMatchingLocationProcessed() {
        ForecastLocation salanda = location("salanda");
        ForecastLocation alleberg = location("alleberg");
        when(forecastLocationService.findAll()).thenReturn(List.of(salanda, alleberg));

        forecastService.runAll(List.of("salanda"));

        verify(forecastProvider).getForecastForLocation(salanda);
        verify(forecastProvider, never()).getForecastForLocation(alleberg);
    }

    @Test
    void runAll_providerCalledForEachLocationInList() {
        ForecastLocation loc1 = location("loc1");
        ForecastLocation loc2 = location("loc2");
        ForecastLocation loc3 = location("loc3");
        when(forecastLocationService.findAll()).thenReturn(List.of(loc1, loc2, loc3));

        forecastService.runAll(List.of("loc1", "loc2", "loc3"));

        verify(forecastProvider).getForecastForLocation(loc1);
        verify(forecastProvider).getForecastForLocation(loc2);
        verify(forecastProvider).getForecastForLocation(loc3);
        verifyNoMoreInteractions(forecastProvider);
    }

    @Test
    void runAll_noMatchingLocation_providerNeverCalled() {
        ForecastLocation salanda = location("salanda");
        when(forecastLocationService.findAll()).thenReturn(List.of(salanda));

        forecastService.runAll(List.of("nonexistent"));

        verify(forecastProvider, never()).getForecastForLocation(any());
    }
}
