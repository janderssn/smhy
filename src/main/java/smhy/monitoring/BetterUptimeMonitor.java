package smhy.monitoring;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

import static org.springframework.util.StringUtils.hasText;

/**
 * Simple service that sends heartbeat requests to a configured url.
 */
@Slf4j
@Service
public class BetterUptimeMonitor {

    private final RestTemplate restTemplate;

    @Value("${betterstack.heartbeat.url:}")
    private String url;

    @Autowired
    public BetterUptimeMonitor(RestTemplateBuilder builder) {
        this(builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build());
    }

    BetterUptimeMonitor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        if (hasText(url)) {
            log.info("Better Uptime heartbeat monitor enabled");
            log.debug("betterstack.heartbeat.url={}", url);
        } else {
            log.info("Better Uptime heartbeat monitor disabled");
            log.info("Configure betterstack.heartbeat.url in application.properties to enable heartbeats");
        }
    }

    /**
     * Sends HEAD request to the configured {@code betterstack.heartbeat.url}.
     */
    public void heartbeat() {
        if (hasText(url)) {
            log.debug("sending heartbeat request to url={}", url);
            try {
                URI uri = UriComponentsBuilder.fromHttpUrl(url).build().toUri();
                HttpHeaders httpHeaders = restTemplate.headForHeaders(uri);
                log.trace(httpHeaders.toString());
            } catch (RestClientException e) {
                log.warn("heartbeat request failed", e);
            }
        } else {
            log.debug("skipping heartbeat request since betterstack.heartbeat.url is not set");
        }
    }
}
