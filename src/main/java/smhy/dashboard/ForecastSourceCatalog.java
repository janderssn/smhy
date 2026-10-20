package smhy.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The forecast source that actually fills the dashboard.
 * Only the selected {@link smhy.forecasts.WeatherForecastProvider} is listed.
 * yr.no is the default. SMHI is listed only when {@code smhy.provider=smhi},
 * because that provider is implemented and is then the one supplying the numbers.
 */
@Component
public class ForecastSourceCatalog {

    private final String activeProvider;

    public ForecastSourceCatalog(@Value("${smhy.provider:yr}") String activeProvider) {
        this.activeProvider = activeProvider == null || activeProvider.isBlank() ? "yr" : activeProvider;
    }

    public String activeProvider() {
        return activeProvider;
    }

    public List<Dashboard.Source> sources() {
        if ("smhi".equals(activeProvider)) {
            return List.of(source("smhi", "SMHI", "https://opendata.smhi.se", null));
        }
        return List.of(source("yr", "yr.no", "https://www.yr.no", "CC BY 4.0"));
    }

    private static Dashboard.Source source(String id, String name, String url, String license) {
        return Dashboard.Source.builder()
                .id(id)
                .name(name)
                .status("live")
                .kind("Prognos")
                .url(url)
                .license(license)
                .build();
    }
}
