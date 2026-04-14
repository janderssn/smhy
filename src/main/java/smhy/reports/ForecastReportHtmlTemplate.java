package smhy.reports;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import smhy.ratings.ForecastRating;
import smhy.ratings.ForecastRatingCollector;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.ZoneId;
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
        templateEngine.process("forecast",
                new Context(Locale.getDefault(),
                        Map.of( "forecast", forecastRating,
                                "forecastDateCET", forecastRating.getForecastDate().atZone(ZoneId.of("CET")),
                                "ratingsByDate", ForecastRatingCollector.ratingsByDay(forecastRating),
                                "page", forecastRating.getForecastLocation())),
                new PrintWriter(printStream));
    }

}
