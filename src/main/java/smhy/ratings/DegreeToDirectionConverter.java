package smhy.ratings;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DegreeToDirectionConverter implements Converter<Double, String> {

    private static final String[] directions = new String[] {
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
    };

    @Override
    public String convert(Double degrees) {
        int index = (int) ((((degrees + 11.25) % 360) / 22.5) % 16);
        return directions[index];
    }
}
