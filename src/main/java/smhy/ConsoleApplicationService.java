package smhy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import smhy.conditions.ForecastConditionService;
import smhy.forecasts.ForecastService;
import smhy.locations.ForecastLocationService;
import smhy.monitoring.BetterUptimeMonitor;
import smhy.reports.ReportService;

import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleApplicationService {

    private final ForecastService forecastService;
    private final BetterUptimeMonitor betterUptimeMonitor;
    private final ReportService reportService;
    private final ForecastLocationService forecastLocationService;
    private final ForecastConditionService forecastConditionService;
    private final JsonExportConfiguration jsonExportConfiguration;

    public void run(ApplicationArguments args) {

        if (args.containsOption("locationsFile")) {
            List<String> locationsFile = args.getOptionValues("locationsFile");
            if (locationsFile.size() == 1) {
                forecastLocationService.load(Paths.get(locationsFile.get(0)));
            }
        }

        if (args.containsOption("conditionsFile")) {
            List<String> conditionsFile = args.getOptionValues("conditionsFile");
            if (conditionsFile.size() == 1) {
                forecastConditionService.load(Paths.get(conditionsFile.get(0)));
            }
        }

        if (args.containsOption("json")) {
            jsonExportConfiguration.setCommandLineOverride(true);
        } else if (args.containsOption("no-json")) {
            jsonExportConfiguration.setCommandLineOverride(false);
        }

        List<String> locations = args.getNonOptionArgs();

        if (args.containsOption("forecast")) {
            forecastService.runAll(locations);
        }

        if (args.containsOption("reports")) {
            reportService.runAll(locations);
        }

        if (args.containsOption("all")) {
            forecastService.runAll(locations);
            reportService.runAll(locations);
        }

        if (args.containsOption("heartbeat")) {
            betterUptimeMonitor.heartbeat();
        }
    }
}
