package smhy.reports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ForecastReportGenerator forecastReportGenerator;

    public void runAll(List<String> locationIds) {
        forecastReportGenerator.generate(locationIds);
    }

}
