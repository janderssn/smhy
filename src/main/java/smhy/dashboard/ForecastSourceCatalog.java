package smhy.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Forecast sources the UI and the JSON API know about.
 * <p>
 * A source is {@code live} only when {@code smhy.provider} selects an implemented
 * {@link smhy.forecasts.WeatherForecastProvider}. Open-Meteo has no provider yet.
 * Holfuy is a live-wind station feed, not a forecast model. Both stay {@code coming},
 * and the dashboard never invents numbers for them.
 * <p>
 * Adding Open-Meteo later means a new {@code WeatherForecastProvider} bean plus an entry
 * here whose id matches {@code smhy.provider}.
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
        return List.of(
                source("yr", "yr.no", "https://www.yr.no", "CC BY 4.0", "Prognos"),
                source("smhi", "SMHI", "https://opendata.smhi.se", null, "Prognos"),
                source("open-meteo", "Open-Meteo", "https://open-meteo.com", null, "Prognos"),
                source("holfuy", "Holfuy", "https://holfuy.com", null, "Live-station")
        );
    }

    private Dashboard.Source source(String id, String name, String url, String license, String kind) {
        boolean implemented = "yr".equals(id) || "smhi".equals(id);
        boolean live = implemented && id.equals(activeProvider);
        return Dashboard.Source.builder()
                .id(id)
                .name(name)
                .status(live ? "live" : "coming")
                .kind(kind)
                .url(url)
                .license(license)
                .build();
    }
}
