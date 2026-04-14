package smhy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import smhy.conditions.ForecastConditionService;
import smhy.forecasts.ForecastService;
import smhy.locations.ForecastLocationService;
import smhy.monitoring.BetterUptimeMonitor;
import smhy.reports.ForecastReportGenerator;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsoleApplicationServiceTest {

    @Mock
    private ForecastService forecastService;

    @Mock
    private BetterUptimeMonitor betterUptimeMonitor;

    @Mock
    private ForecastReportGenerator forecastReportGenerator;

    @Mock
    private ForecastLocationService forecastLocationService;

    @Mock
    private ForecastConditionService forecastConditionService;

    @Mock
    private JsonExportConfiguration jsonExportConfiguration;

    @InjectMocks
    private ConsoleApplicationService service;

    @Test
    void forecastFlag_callsForecastServiceRunAll() {
        service.run(new DefaultApplicationArguments("--forecast", "salanda"));
        verify(forecastService).runAll(List.of("salanda"));
        verifyNoInteractions(forecastReportGenerator, betterUptimeMonitor);
    }

    @Test
    void reportsFlag_callsReportServiceRunAll() {
        service.run(new DefaultApplicationArguments("--reports", "salanda"));
        verify(forecastReportGenerator).generate(List.of("salanda"));
        verifyNoInteractions(forecastService, betterUptimeMonitor);
    }

    @Test
    void allFlag_callsBothForecastServiceAndReportService() {
        service.run(new DefaultApplicationArguments("--all", "salanda"));
        verify(forecastService).runAll(List.of("salanda"));
        verify(forecastReportGenerator).generate(List.of("salanda"));
        verifyNoInteractions(betterUptimeMonitor);
    }

    @Test
    void heartbeatFlag_callsBetterUptimeMonitorHeartbeat() {
        service.run(new DefaultApplicationArguments("--heartbeat"));
        verify(betterUptimeMonitor).heartbeat();
        verifyNoInteractions(forecastService, forecastReportGenerator);
    }

    @Test
    void jsonFlag_setsCommandLineOverrideTrue() {
        service.run(new DefaultApplicationArguments("--json"));
        verify(jsonExportConfiguration).setCommandLineOverride(true);
        verifyNoInteractions(forecastService, forecastReportGenerator, betterUptimeMonitor);
    }

    @Test
    void noJsonFlag_setsCommandLineOverrideFalse() {
        service.run(new DefaultApplicationArguments("--no-json"));
        verify(jsonExportConfiguration).setCommandLineOverride(false);
        verifyNoInteractions(forecastService, forecastReportGenerator, betterUptimeMonitor);
    }

    @Test
    void noFlags_noneOfTheServicesAreCalled() {
        service.run(new DefaultApplicationArguments());
        verifyNoInteractions(forecastService, forecastReportGenerator, betterUptimeMonitor);
        verify(jsonExportConfiguration, never()).setCommandLineOverride(any());
    }

    @Test
    void locationsFileFlag_callsForecastLocationServiceLoad() {
        service.run(new DefaultApplicationArguments("--locationsFile=/tmp/locations.yml"));
        verify(forecastLocationService).load(java.nio.file.Paths.get("/tmp/locations.yml"));
        verifyNoInteractions(forecastService, forecastReportGenerator, betterUptimeMonitor);
    }

    @Test
    void conditionsFileFlag_callsForecastConditionServiceLoad() {
        service.run(new DefaultApplicationArguments("--conditionsFile=/tmp/conditions.yml"));
        verify(forecastConditionService).load(java.nio.file.Paths.get("/tmp/conditions.yml"));
        verifyNoInteractions(forecastService, forecastReportGenerator, betterUptimeMonitor);
    }
}
