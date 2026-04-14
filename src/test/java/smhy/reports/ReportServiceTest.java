package smhy.reports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ForecastReportGenerator forecastReportGenerator;

    @InjectMocks
    private ReportService reportService;

    @Test
    void runAllWithLocationIds_callsGenerateWithLocationIds() {
        List<String> locationIds = List.of("salanda");
        reportService.runAll(locationIds);
        verify(forecastReportGenerator).generate(locationIds);
    }

    @Test
    void runAllWithEmptyList_callsGenerateWithEmptyList() {
        List<String> locationIds = List.of();
        reportService.runAll(locationIds);
        verify(forecastReportGenerator).generate(locationIds);
    }
}
