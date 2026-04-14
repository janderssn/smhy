package smhy.conditions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForecastConditionServiceTest {

    @Mock
    ForecastConditionYamlFileStore forecastConditionYamlFileStore;

    @InjectMocks
    ForecastConditionService forecastConditionService;

    @Test
    void loadPath_storesConditionsFromYamlFile() {
        Path path = Path.of("/some/path/conditions.yml");
        List<ForecastCondition> conditions = List.of(
                ForecastCondition.builder().id("c1").location("salanda").score(80.0).build()
        );
        when(forecastConditionYamlFileStore.load(path)).thenReturn(conditions);

        forecastConditionService.load(path);

        verify(forecastConditionYamlFileStore).load(path);
        assertThat(forecastConditionService.findByLocation("salanda")).hasSize(1);
    }

    @Test
    void loadPath_findByLocationFiltersCorrectly() {
        Path path = Path.of("/some/path/conditions.yml");
        List<ForecastCondition> conditions = List.of(
                ForecastCondition.builder().id("c1").location("salanda").score(80.0).build(),
                ForecastCondition.builder().id("c2").location("alleberg").score(100.0).build()
        );
        when(forecastConditionYamlFileStore.load(path)).thenReturn(conditions);

        forecastConditionService.load(path);

        assertThat(forecastConditionService.findByLocation("salanda")).hasSize(1);
        assertThat(forecastConditionService.findByLocation("alleberg")).hasSize(1);
        assertThat(forecastConditionService.findByLocation("unknown")).isEmpty();
    }

    @Test
    void loadPathWithEmpty_findByLocationReturnsEmpty() {
        Path path = Path.of("/some/path/conditions.yml");
        when(forecastConditionYamlFileStore.load(path)).thenReturn(List.of());

        forecastConditionService.load(path);

        assertThat(forecastConditionService.findByLocation("salanda")).isEmpty();
    }

    @Test
    void loadDefaultConditions_loadsFromConfigFile() throws Exception {
        Resource resource = new ClassPathResource("config/conditions.yml");
        List<ForecastCondition> conditions = List.of(
                ForecastCondition.builder().id("d1").location("salanda").score(90.0).build()
        );
        when(forecastConditionYamlFileStore.load(resource)).thenReturn(conditions);

        ReflectionTestUtils.setField(forecastConditionService, "conditionsConfigFile", resource);
        Method m = ForecastConditionService.class.getDeclaredMethod("loadDefaultConditions");
        m.setAccessible(true);
        m.invoke(forecastConditionService);

        verify(forecastConditionYamlFileStore).load(resource);
        assertThat(forecastConditionService.findByLocation("salanda")).hasSize(1);
    }
}
