package smhy.reports;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import smhy.ratings.ForecastRating;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class ForecastReportHtmlTemplate {

    private final SpringTemplateEngine templateEngine;

    public void generate(ForecastRating forecastRating) {
        generate(forecastRating, System.out);
    }

    public void generate(ForecastRating forecastRating, Path outputPath) throws IOException {
        try (PrintStream printStream = new PrintStream(outputPath.toFile())) {
            generate(forecastRating, printStream);
        }
    }

    public void generate(ForecastRating forecastRating, PrintStream printStream) {
        templateEngine.process("app",
                new Context(Locale.getDefault(),
                        Map.of("view", viewOf(forecastRating),
                                "page", forecastRating.getForecastLocation() == null ? "Vecka" : forecastRating.getForecastLocation())),
                new PrintWriter(printStream));
    }

    private static String viewOf(ForecastRating forecastRating) {
        String location = forecastRating.getForecastLocation();
        if (location == null) {
            return "vecka";
        }
        return switch (location.toLowerCase(Locale.ROOT)) {
            case "vecka" -> "vecka";
            case "helg" -> "helg";
            case "favoriter" -> "favoriter";
            default -> "region";
        };
    }

}
