package smhy.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import smhy.ratings.ForecastRating;

import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ForecastReportHtmlTemplateTest {

    @Mock
    private SpringTemplateEngine templateEngine;

    @TempDir
    Path tempDir;

    private ForecastReportHtmlTemplate template;

    @BeforeEach
    void setUp() {
        doNothing().when(templateEngine).process(any(String.class), any(IContext.class), any(Writer.class));
        template = new ForecastReportHtmlTemplate(templateEngine);
    }

    private ForecastRating buildRating() {
        return ForecastRating.builder()
                .forecastDate(Instant.now())
                .forecastLocation("test")
                .siteRatings(List.of())
                .build();
    }

    @Test
    void generate_withForecastRating_callsTemplateEngineWithForecastTemplate() {
        ForecastRating rating = buildRating();

        template.generate(rating);

        verify(templateEngine).process(eq("app"), any(IContext.class), any(Writer.class));
    }

    @Test
    void generate_withForecastRatingAndPrintStream_callsTemplateEngineWithForecastTemplate() {
        ForecastRating rating = buildRating();

        template.generate(rating, System.out);

        verify(templateEngine).process(eq("app"), any(IContext.class), any(Writer.class));
    }

    @Test
    void generate_withForecastRatingAndPath_callsTemplateEngineWithForecastTemplate() throws Exception {
        ForecastRating rating = buildRating();
        Path outputFile = tempDir.resolve("output.html");

        template.generate(rating, outputFile);

        verify(templateEngine).process(eq("app"), any(IContext.class), any(Writer.class));
    }
}
