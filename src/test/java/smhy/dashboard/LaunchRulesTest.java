package smhy.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smhy.conditions.ForecastConditionService;
import smhy.conditions.ForecastConditionYamlFileStore;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchRulesTest {

    private LaunchRules launchRules;

    @BeforeEach
    void setUp() {
        ForecastConditionService conditions = new ForecastConditionService(new ForecastConditionYamlFileStore());
        conditions.load(Path.of("src/main/resources/config/conditions.yml"));
        launchRules = new LaunchRules(conditions);
    }

    @Test
    void octoberAllebergKeepsWestAndEastSectors() {
        Dashboard.Launch launch = launchRules.forLocation("alleberg", 10);

        assertEquals(List.of("ENE", "E", "ESE", "WNW", "NW"), launch.getGood());
        assertEquals(List.of("NE", "WSW", "W", "NNW"), launch.getOk());
        assertEquals(5.0, launch.getWindMin());
        assertEquals(6.0, launch.getWindMax());
        assertEquals(11.0, launch.getGustMax());
    }

    @Test
    void octoberSalandaDropsTheSummerOnlyRule() {
        Dashboard.Launch launch = launchRules.forLocation("salanda", 10);

        assertEquals(List.of("N", "SE", "SSE", "S", "NW", "NNW"), launch.getGood());
        assertEquals(List.of("NNE", "NE", "SSW"), launch.getOk());
        assertEquals(1.0, launch.getWindMin());
        assertEquals(5.0, launch.getWindMax());
        assertEquals(7.0, launch.getGustMax());
    }

    @Test
    void octoberUttranIgnoresTheCatchAll() {
        Dashboard.Launch launch = launchRules.forLocation("uttran", 10);

        assertEquals(List.of("S", "SSW"), launch.getGood());
        assertTrue(launch.getOk().isEmpty());
        assertEquals(2.0, launch.getWindMin());
        assertEquals(4.0, launch.getWindMax());
    }

    @Test
    void octoberAreskutanKeepsSouthAndDropsWinterNorth() {
        Dashboard.Launch launch = launchRules.forLocation("areskutan", 10);

        assertEquals(List.of("SE", "SSE", "S", "SSW"), launch.getGood());
        assertFalse(launch.getGood().contains("NW"));
        assertTrue(launch.getOk().isEmpty());
    }
}
