package smhy.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetterUptimeMonitorTest {

    @Mock
    RestTemplate restTemplate;

    // -----------------------------------------------------------------------
    // heartbeat() — url is blank / empty
    // -----------------------------------------------------------------------

    @Test
    void heartbeatDoesNotCallRestTemplateWhenUrlIsEmpty() {
        BetterUptimeMonitor monitor = new BetterUptimeMonitor(restTemplate);
        ReflectionTestUtils.setField(monitor, "url", "");

        monitor.heartbeat();

        verify(restTemplate, never()).headForHeaders(any(URI.class));
    }

    @Test
    void heartbeatDoesNotCallRestTemplateWhenUrlIsBlank() {
        BetterUptimeMonitor monitor = new BetterUptimeMonitor(restTemplate);
        ReflectionTestUtils.setField(monitor, "url", "   ");

        monitor.heartbeat();

        verify(restTemplate, never()).headForHeaders(any(URI.class));
    }

    // -----------------------------------------------------------------------
    // heartbeat() — url is set
    // -----------------------------------------------------------------------

    @Test
    void heartbeatCallsRestTemplateWithCorrectUriWhenUrlIsSet() {
        BetterUptimeMonitor monitor = new BetterUptimeMonitor(restTemplate);
        String heartbeatUrl = "http://example.com/heartbeat";
        ReflectionTestUtils.setField(monitor, "url", heartbeatUrl);
        when(restTemplate.headForHeaders(any(URI.class))).thenReturn(new HttpHeaders());

        monitor.heartbeat();

        verify(restTemplate).headForHeaders(URI.create(heartbeatUrl));
    }

    @Test
    void heartbeatSwallowsRestClientExceptionAndDoesNotPropagate() {
        BetterUptimeMonitor monitor = new BetterUptimeMonitor(restTemplate);
        ReflectionTestUtils.setField(monitor, "url", "http://example.com/heartbeat");
        when(restTemplate.headForHeaders(any(URI.class)))
                .thenThrow(new org.springframework.web.client.RestClientException("timeout"));

        // should not throw
        monitor.heartbeat();

        verify(restTemplate).headForHeaders(any(URI.class));
    }

    // -----------------------------------------------------------------------
    // init() — url is blank / empty
    // -----------------------------------------------------------------------

    @Test
    void initDoesNotThrowWhenUrlIsBlank() {
        BetterUptimeMonitor monitor = new BetterUptimeMonitor(restTemplate);
        ReflectionTestUtils.setField(monitor, "url", "");

        // should complete without throwing
        monitor.init();
    }

    @Test
    void initDoesNotThrowWhenUrlIsWhitespace() {
        BetterUptimeMonitor monitor = new BetterUptimeMonitor(restTemplate);
        ReflectionTestUtils.setField(monitor, "url", "   ");

        monitor.init();
    }

    // -----------------------------------------------------------------------
    // init() — url is set
    // -----------------------------------------------------------------------

    @Test
    void initDoesNotThrowWhenUrlIsSet() {
        BetterUptimeMonitor monitor = new BetterUptimeMonitor(restTemplate);
        ReflectionTestUtils.setField(monitor, "url", "http://example.com/heartbeat");

        monitor.init();
    }
}
