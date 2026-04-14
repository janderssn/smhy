package smhy.forecasts.smhi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import smhy.forecasts.WeatherForecast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

class SmhiWeatherForecastConverterTest {

    private SmhiWeatherForecastConverter converter = new SmhiWeatherForecastConverter();
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void convertCompleteForecast() throws Exception {
        WeatherForecast actualForecast = converter.convert(readJsonFromFile("forecast.json"));
        WeatherForecast.WeatherForecastItem forecastItem = actualForecast.getItems().get(0);
        Assertions.assertEquals(46.0, forecastItem.getWindDirection());
        Assertions.assertEquals(2.3, forecastItem.getWindSpeed());
        Assertions.assertEquals(5.5, forecastItem.getWindGusts());
        Assertions.assertEquals(1016.4, forecastItem.getAirPressure());
        Assertions.assertEquals(-1.1, forecastItem.getAirTemperature());
        Assertions.assertEquals(0.0, forecastItem.getPrecipitationAmountMin());
        Assertions.assertEquals(0.0, forecastItem.getPrecipitationAmountMax());
        Assertions.assertEquals(0.0, forecastItem.getPrecipitationAmount());
        Assertions.assertEquals(0.0, forecastItem.getProbabilityPrecipitation());
        Assertions.assertEquals(0.0, forecastItem.getProbabilityThunder());
    }

    private ObjectNode readJsonFromFile(String filename) throws Exception {
        String content = Files.readString(Paths.get("src/test/resources/smhi",filename));
        return (ObjectNode) mapper.readTree(content);
    }
}