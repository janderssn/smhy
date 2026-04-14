package smhy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!junit")
@Component
@RequiredArgsConstructor
public class ConsoleApplicationRunner implements ApplicationRunner {

    private final ConsoleApplicationService consoleApplicationService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("started");
        consoleApplicationService.run(args);
        log.info("finished");
    }

}
