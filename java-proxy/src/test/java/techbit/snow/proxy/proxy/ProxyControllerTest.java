package techbit.snow.proxy.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import techbit.snow.proxy.error.InvalidRequestException;
import techbit.snow.proxy.snow.stream.SnowStream.ConsumerThreadException;
import techbit.snow.proxy.snow.transcoding.PlainTextStreamEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyControllerTest {

    @Mock
    private ProxyService streaming;

    @Mock
    private OutputStream out;

    @Mock
    private PlainTextStreamEncoder textStreamEncoder;
    @Mock
    private HttpServletRequest request;
    private ProxyController controller;


    @BeforeEach
    void setup() {
        lenient().when(request.getScheme()).thenReturn("https");
        lenient().when(request.getServerName()).thenReturn("domain.com");
        lenient().when(request.getServerPort()).thenReturn(1234);
        lenient().when(streaming.sessionDetails("session-abc")).thenReturn(Map.of(
                "key1", "value1",
                "key2", "value2"
        ));

        controller = new ProxyController(streaming, textStreamEncoder);
    }


    @Test
    void whenNotEnoughParamsRequested_thenThrowException() {
        assertThrows(InvalidRequestException.class, controller::insufficientParams);
    }

    @Test
    void givenNoCustomConfiguration_whenStartSession_thenStreamWithEmptyConfigMap() throws IOException, InterruptedException {
        Map<?, ?> details = controller.startSession("session-abc", "", request);

        verify(streaming).startSession("session-abc", Map.of());
        assertFalse(details.isEmpty());
    }

    @Test
    void givenNoCustomConfiguration_whenStartSession_thenProvideDetails() throws IOException, InterruptedException {
        when(streaming.hasSession("session-abc")).thenReturn(true);
        when(streaming.isSessionRunning("session-abc")).thenReturn(true);

        Map<?, ?> details = controller.startSession(
                "session-abc", "/", request);

        assertFalse(details.isEmpty());
        assertExpectedDetails(details);
    }

    @Test
    void givenCustomConfiguration_whenStartSession_thenStartSessionWithCustomParams() throws IOException, InterruptedException {
        when(streaming.hasSession("session-abc")).thenReturn(true);
        when(streaming.isSessionRunning("session-abc")).thenReturn(true);

        controller.startSession(
                "session-abc", "/my1/val1/my2/val2/", request);

        verify(streaming).startSession("session-abc", Map.of(
                "my1", "val1",
                "my2", "val2"
        ));
    }

    @Test
    void whenAskingForDetails_thenProvideDetails() {
        when(streaming.hasSession("session-abc")).thenReturn(true);
        when(streaming.isSessionRunning("session-abc")).thenReturn(true);

        Map<?, ?> details = controller.streamDetails(
                "session-abc", request);

        assertExpectedDetails(details);
    }

    @Test
    void givenNoCustomConfiguration_whenStreamTextToClient_thenStreamWithEmptyConfigMap() throws IOException, InterruptedException, ExecutionException, ConsumerThreadException {
        controller.streamTextToClient("session-abc", "").get().writeTo(out);

        verify(streaming).streamSessionTo("session-abc", out, textStreamEncoder, Map.of());
    }

    @Test
    void givenCustomConfiguration_whenStreamTextToClient_thenStreamWithProperConfigMap() throws IOException, InterruptedException, ExecutionException, ConsumerThreadException {
        controller.streamTextToClient("session-abc", "/key1/value1/key2/value2").get().writeTo(out);

        verify(streaming).streamSessionTo("session-abc", out, textStreamEncoder, Map.of(
                "key1", "value1",
                "key2", "value2"
        ));
    }

    @Test
    void givenCustomConfigurationWithMissingValue_whenStreamToClient_thenThrowException() throws InterruptedException, ExecutionException {
        StreamingResponseBody responseBody = controller.streamTextToClient(
                "session-abc", "/key1/value1/key2/").get();

        assertThrows(InvalidRequestException.class, () -> responseBody.writeTo(out));
    }

    @Test
    void givenCustomConfigurationWithEmptyValues_whenStreamToClient_thenThrowException() throws InterruptedException, ExecutionException {
        StreamingResponseBody responseBody = controller.streamTextToClient(
                "session-abc", "/key1//key2/value1/").get();

        assertThrows(InvalidRequestException.class, () -> responseBody.writeTo(out));
    }

    @Test
    void whenStreamDetails_thenValidDetailsResponded() {
        when(streaming.hasSession("session-abc")).thenReturn(true);
        when(streaming.isSessionRunning("session-abc")).thenReturn(true);

        Map<String, Object> response = controller.streamDetails("session-abc", request);

        assertExpectedDetails(response);
    }

    @Test
    void whenStopStreamingRequested_thenStopStream() throws IOException, InterruptedException {
        controller.stopStreaming("session-abc");

        verify(streaming).stopSession("session-abc");
    }

    @Test
    void whenStopStreamingRequested_thenRespondWithInfoMap() throws IOException, InterruptedException {
        Map<String, Object> response = controller.stopStreaming("session-abc");

        assertEquals(Map.of(
                "status", true,
                "sessionId", "session-abc",
                "running", false
        ), response);
    }

    @Test
    void whenClientAbortDuringStreaming_thenNoErrorOccurs() throws IOException, InterruptedException, ConsumerThreadException {
        doThrow(ClientAbortException.class).when(streaming).streamSessionTo(
                "session-abc", out, textStreamEncoder, Map.of());

        assertDoesNotThrow(() -> controller.streamTextToClient("session-abc", "").get().writeTo(out));
    }

    @Test
    void whenThreadInterruptedDuringStreaming_thenErrorOccurs() throws IOException, InterruptedException, ConsumerThreadException {
        doThrow(InterruptedException.class).when(streaming).streamSessionTo(
                "session-abc", out, textStreamEncoder, Map.of());

        assertThrows(IOException.class, () -> controller.streamTextToClient("session-abc", "").get().writeTo(out));
    }

    private void assertExpectedDetails(Map<?, ?> details) {
        assertEquals(Map.of(
                "status", true,
                "exists", true,
                "running", true,
                "sessionId", "session-abc",
                "streamTextUrl", "https://domain.com:1234/text/session-abc",
                "streamWebsocketsStompBrokerUrl", "ws://domain.com:1234/ws/",
                "streamWebsocketsUrl", "/app/stream/session-abc",
                "key1", "value1",
                "key2", "value2"
        ), details);
    }
}