package smhy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsoleApplicationRunnerTest {

    @Mock
    private ConsoleApplicationService consoleApplicationService;

    @InjectMocks
    private ConsoleApplicationRunner runner;

    @Test
    void run_delegatesToConsoleApplicationService() throws Exception {
        DefaultApplicationArguments args = new DefaultApplicationArguments("--forecast");
        runner.run(args);
        verify(consoleApplicationService).run(args);
    }

    @Test
    void run_withNoArgs_delegatesToConsoleApplicationService() throws Exception {
        DefaultApplicationArguments args = new DefaultApplicationArguments();
        runner.run(args);
        verify(consoleApplicationService).run(args);
    }
}
