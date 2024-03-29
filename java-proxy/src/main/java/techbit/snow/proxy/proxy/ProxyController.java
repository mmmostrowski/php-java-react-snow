package techbit.snow.proxy.proxy;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import techbit.snow.proxy.error.InvalidRequestException;
import techbit.snow.proxy.snow.stream.SnowStream;
import techbit.snow.proxy.snow.transcoding.PlainTextStreamEncoder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.stream;

@Log4j2
@RestController
public class ProxyController {

    private final ProxyService streaming;
    private final PlainTextStreamEncoder textStreamEncoder;

    public ProxyController(ProxyService streaming, PlainTextStreamEncoder textStreamEncoder) {
        this.streaming = streaming;
        this.textStreamEncoder = textStreamEncoder;
    }

    @GetMapping({"/", "/start", "/start/", "/text", "/text/", "/stop", "/stop/", "/details/", "/details"})
    public void insufficientParams() {
        throw new InvalidRequestException(
                "Invalid url! Url Should follow pattern: https://domain.com/<action>/<session-id>");
    }

    @GetMapping("/start/{sessionId}/{*configuration}")
    public Map<String, Object> startSession(
            @PathVariable String sessionId,
            @PathVariable String configuration, HttpServletRequest request) throws IOException, InterruptedException {
        log.debug("startSession( {}, {} )", sessionId, configuration.isBlank() ? "<default-config>" : configuration);

        streaming.startSession(sessionId, toConfMap(configuration));

        return streamDetails(sessionId, request);
    }

    @Async("streamAsyncTaskExecutor")
    @GetMapping("/text/{sessionId}/{*configuration}")
    public CompletableFuture<StreamingResponseBody> streamTextToClient(
            @PathVariable String sessionId,
            @PathVariable String configuration
    ) {
        log.debug("streamTextToClient( {}, {} )", sessionId, configuration);

        return CompletableFuture.supplyAsync(() -> out -> {
            try {
                log.debug("streamTextToClient( {} ) | Async Start ", sessionId);

                streaming.streamSessionTo(sessionId, out, textStreamEncoder, toConfMap(configuration));

                log.debug("streamTextToClient( {} ) | Async Finished", sessionId);
            } catch (ClientAbortException e) {
                log.debug("streamTextToClient( {} ) | Client aborted", sessionId);
            } catch (InterruptedException | SnowStream.ConsumerThreadException e) {
                log.error("streamTextToClient( {} ) | Error occurred", sessionId);
                throw new IOException("Streaming interrupted ", e);
            }
        });
    }

    @GetMapping({"/stop/{sessionId}", "/stop/{sessionId}/"})
    public Map<String, Object> stopStreaming(@PathVariable String sessionId) throws IOException, InterruptedException {
        log.debug("stopStreaming( {} )", sessionId);

        streaming.stopSession(sessionId);

        return Map.of(
                "status", true,
                "sessionId", sessionId,
                "running", false
        );
    }

    @GetMapping({"/details/{sessionId}", "/details/{sessionId}/"})
    public Map<String, Object> streamDetails(@PathVariable String sessionId, HttpServletRequest request) {
        log.debug("streamDetails( {} )", sessionId);

        Map<String, Object> map = Maps.newHashMap(streaming.sessionDetails(sessionId));

        map.putAll(Map.of(
                "status", true,
                "sessionId", sessionId,
                "exists", streaming.hasSession(sessionId),
                "running", streaming.isSessionRunning(sessionId),
                "streamTextUrl", urlTo(request, "/text/" + sessionId),
                "streamWebsocketsStompBrokerUrl", urlTo("ws://", request, "/ws/"),
                "streamWebsocketsUrl", "/app/stream/" + sessionId
        ));

        return map;
    }

    private Map<String, String> toConfMap(String configuration) {
        if (configuration.isBlank() || configuration.equals("/")) {
            return Map.of();
        }

        final String[] elements = configuration.substring(1).split("/");
        boolean hasOddNumberOfElements = (elements.length & 1) != 0;
        if (hasOddNumberOfElements || stream(elements).anyMatch(String::isBlank)) {
            throw new InvalidRequestException("Please provide request in form: " +
                    "https://domain.com/sessionId/key1/val1/key2/val2/...");
        }

        final Map<String, String> confMap = Maps.newHashMap();
        for (int i = 0; i < elements.length; i += 2) {
            final String key = elements[i];
            final String value = elements[i + 1];

            confMap.put(key, value);
        }
        return confMap;
    }

    private String urlTo(HttpServletRequest request, String location) {
        return urlTo(request.getScheme() + "://", request, location);
    }

    private String urlTo(String scheme, HttpServletRequest request, String location) {
        return scheme + request.getServerName() + ':' + request.getServerPort() + location;
    }
}
