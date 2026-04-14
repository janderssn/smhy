package smhy.locations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import smhy.util.AbstractYamlFileStore;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastLocationYamlFileStore extends AbstractYamlFileStore<ForecastLocation> {

}
