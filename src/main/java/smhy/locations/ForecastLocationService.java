package smhy.locations;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastLocationService {

    private final ForecastLocationYamlFileStore forecastLocationYamlFileStore;

    @Value("${smhy.config.dir}/locations.yml")
    private Resource locationsConfigFile;

    private List<ForecastLocation> locations = new ArrayList<>();

    @PostConstruct
    private void init() {
        log.info("Loading locations from locations.yml");
        locations = new ArrayList<>(forecastLocationYamlFileStore.load(locationsConfigFile));
        log.info("Found {} locations", locations.size());
    }

    public void load(Path path) {
        locations = new ArrayList<>(forecastLocationYamlFileStore.load(path));
        log.info("Found {} locations", locations.size());
    }

    public List<ForecastLocation> findAll() {
        return Collections.unmodifiableList(locations);
    }

    public Optional<ForecastLocation> findById(String id) {
        return locations.stream().filter(l -> id.equals(l.getId())).findFirst();
    }
}
