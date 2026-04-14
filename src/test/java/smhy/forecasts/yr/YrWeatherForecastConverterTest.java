package smhy.forecasts.yr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import smhy.forecasts.WeatherForecast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

class YrWeatherForecastConverterTest {

    private YrWeatherForecastConverter converter = new YrWeatherForecastConverter();
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void convertCompleteForecast() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast.json"));
    }

    @Test
    void convertForecastWithNextOneSixAndTwelveHours() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast_1_6_12.json"));
        Assertions.assertEquals(1.0, actualForecast.getItems().get(0).getPrecipitationAmountMin());
        Assertions.assertEquals(2.0, actualForecast.getItems().get(0).getPrecipitationAmountMax());
        Assertions.assertEquals(0.5, actualForecast.getItems().get(0).getPrecipitationAmount());
        Assertions.assertEquals(0.1, actualForecast.getItems().get(0).getProbabilityPrecipitation());
        Assertions.assertEquals(0.2, actualForecast.getItems().get(0).getProbabilityThunder());
    }

    @Test
    void convertForecastWithOnlyNextOneHours() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast_1.json"));
        Assertions.assertEquals(1.0, actualForecast.getItems().get(0).getPrecipitationAmountMin());
        Assertions.assertEquals(2.0, actualForecast.getItems().get(0).getPrecipitationAmountMax());
        Assertions.assertEquals(0.5, actualForecast.getItems().get(0).getPrecipitationAmount());
        Assertions.assertEquals(0.1, actualForecast.getItems().get(0).getProbabilityPrecipitation());
        Assertions.assertEquals(0.2, actualForecast.getItems().get(0).getProbabilityThunder());
    }

    @Test
    void convertForecastWithOnlyNextOneAndSixHours() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast_1_6.json"));
        Assertions.assertEquals(1.0, actualForecast.getItems().get(0).getPrecipitationAmountMin());
        Assertions.assertEquals(2.0, actualForecast.getItems().get(0).getPrecipitationAmountMax());
        Assertions.assertEquals(0.5, actualForecast.getItems().get(0).getPrecipitationAmount());
        Assertions.assertEquals(0.1, actualForecast.getItems().get(0).getProbabilityPrecipitation());
        Assertions.assertEquals(0.2, actualForecast.getItems().get(0).getProbabilityThunder());
    }

    @Test
    void convertForecastWithOnlyNextSixAndTwelveHours() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast_6_12.json"));
        Assertions.assertEquals(1.0, actualForecast.getItems().get(0).getPrecipitationAmountMin());
        Assertions.assertEquals(2.0, actualForecast.getItems().get(0).getPrecipitationAmountMax());
        Assertions.assertEquals(0.5, actualForecast.getItems().get(0).getPrecipitationAmount());
        Assertions.assertEquals(0.1, actualForecast.getItems().get(0).getProbabilityPrecipitation());
        Assertions.assertEquals(0.0, actualForecast.getItems().get(0).getProbabilityThunder());
    }

    @Test
    void convertForecastWithOnlyNextSixHours() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast_6.json"));
        Assertions.assertEquals(1.0, actualForecast.getItems().get(0).getPrecipitationAmountMin());
        Assertions.assertEquals(2.0, actualForecast.getItems().get(0).getPrecipitationAmountMax());
        Assertions.assertEquals(0.5, actualForecast.getItems().get(0).getPrecipitationAmount());
        Assertions.assertEquals(0.1, actualForecast.getItems().get(0).getProbabilityPrecipitation());
        Assertions.assertEquals(0.0, actualForecast.getItems().get(0).getProbabilityThunder());
    }

    @Test
    void convertForecastWithOnlyNextTwelveHours() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast_12.json"));
        Assertions.assertEquals(0, actualForecast.getItems().get(0).getPrecipitationAmountMin());
        Assertions.assertEquals(0, actualForecast.getItems().get(0).getPrecipitationAmountMax());
        Assertions.assertEquals(0, actualForecast.getItems().get(0).getPrecipitationAmount());
        Assertions.assertEquals(30.9, actualForecast.getItems().get(0).getProbabilityPrecipitation());
        Assertions.assertEquals(0, actualForecast.getItems().get(0).getProbabilityThunder());
    }

    private ObjectNode readJsonFromFile(String filename) throws Exception {
        String content = Files.readString(Paths.get("src/test/resources/yr",filename));
        return (ObjectNode) mapper.readTree(content);
    }
}