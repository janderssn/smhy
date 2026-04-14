package smhy.locations;

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
class ForecastLocationServiceTest {

    @Mock
    ForecastLocationYamlFileStore forecastLocationYamlFileStore;

    @InjectMocks
    ForecastLocationService forecastLocationService;

    @Test
    void loadPath_storesLocationsFromYamlFile() {
        Path path = Path.of("/some/path/locations.yml");
        List<ForecastLocation> locations = List.of(
                ForecastLocation.builder().id("l1").name("salanda").region("Västra Götaland").build()
        );
        when(forecastLocationYamlFileStore.load(path)).thenReturn(locations);

        forecastLocationService.load(path);

        verify(forecastLocationYamlFileStore).load(path);
        assertThat(forecastLocationService.findAll()).isEqualTo(locations);
    }

    @Test
    void loadPath_findByIdReturnsMatchingLocation() {
        Path path = Path.of("/some/path/locations.yml");
        ForecastLocation loc = ForecastLocation.builder().id("salanda").name("Sälanda").build();
        when(forecastLocationYamlFileStore.load(path)).thenReturn(List.of(loc));

        forecastLocationService.load(path);

        assertThat(forecastLocationService.findById("salanda")).contains(loc);
        assertThat(forecastLocationService.findById("unknown")).isEmpty();
    }

    @Test
    void loadPathWithEmpty_findAllReturnsEmptyList() {
        Path path = Path.of("/some/path/locations.yml");
        when(forecastLocationYamlFileStore.load(path)).thenReturn(List.of());

        forecastLocationService.load(path);

        assertThat(forecastLocationService.findAll()).isEmpty();
    }

    @Test
    void init_loadsLocationsFromConfigFile() throws Exception {
        Resource resource = new ClassPathResource("config/locations.yml");
        List<ForecastLocation> locations = List.of(
                ForecastLocation.builder().id("salanda").name("Salanda").region("se/stockholm").build()
        );
        when(forecastLocationYamlFileStore.load(resource)).thenReturn(locations);

        ReflectionTestUtils.setField(forecastLocationService, "locationsConfigFile", resource);
        Method m = ForecastLocationService.class.getDeclaredMethod("init");
        m.setAccessible(true);
        m.invoke(forecastLocationService);

        verify(forecastLocationYamlFileStore).load(resource);
        assertThat(forecastLocationService.findAll()).isEqualTo(locations);
    }
}
