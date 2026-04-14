package smhy.conditions;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastConditionService {

    private final ForecastConditionYamlFileStore forecastConditionYamlFileStore;

    @Value("${smhy.config.dir}/conditions.yml")
    private Resource conditionsConfigFile;

    private List<ForecastCondition> conditions = new ArrayList<>();

    @PostConstruct
    private void loadDefaultConditions() {
        log.info("Loading conditions from conditions.yml");
        load(conditionsConfigFile);
    }

    public void load(Path path) {
        conditions = new ArrayList<>(forecastConditionYamlFileStore.load(path));
        log.info("Found {} conditions", conditions.size());
    }

    private void load(Resource resource) {
        conditions = new ArrayList<>(forecastConditionYamlFileStore.load(resource));
        log.info("Found {} conditions", conditions.size());
    }

    public List<ForecastCondition> findByLocation(String location) {
        return conditions.stream().filter(c -> location.equals(c.getLocation())).toList();
    }
}
