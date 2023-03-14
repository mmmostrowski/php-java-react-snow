package techbit.snow.proxy.service.stream;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import techbit.snow.proxy.dto.SnowAnimationMetadata;
import techbit.snow.proxy.dto.SnowDataFrame;
import techbit.snow.proxy.service.phpsnow.PhpSnowApp;
import techbit.snow.proxy.service.phpsnow.PhpSnowConfig;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Service
@Scope(SCOPE_PROTOTYPE)
@Log4j2
public class SnowStream {

    private final String sessionId;

    private final SnowDataBuffer buffer;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final PhpSnowApp phpSnow;

    private final NamedPipe pipe;

    private final PhpSnowConfig phpSnowConfig;

    private SnowAnimationMetadata metadata;

    private boolean running = false;

    @Autowired
    @Qualifier("phpsnowConfig.create")
    private ObjectProvider<PhpSnowConfig> configProvider;

    public SnowStream(String sessionId, PhpSnowConfig phpSnowConfig,
                      NamedPipe pipe, PhpSnowApp phpSnow, SnowDataBuffer buffer) {
        this.sessionId = sessionId;
        this.phpSnowConfig = phpSnowConfig;
        this.pipe = pipe;
        this.phpSnow = phpSnow;
        this.buffer = buffer;
    }

    public void startPhpApp() throws IOException {
        stopPhpApp();

        phpSnow.start();
    }

    public boolean isActive() {
        return running && phpSnow.isAlive();
    }

    public void stopPhpApp() throws IOException {
        phpSnow.stop();
        pipe.destroy();
        running = false;
        metadata = null;
    }

    public void startConsumingSnowData() throws InterruptedException, IOException {
        log.debug("startConsumingSnowData( {} ) | Opening pipe stream", sessionId);
        FileInputStream stream = pipe.inputStream();

        log.debug("startConsumingSnowData( {} ) | Reading metadata", sessionId);
        metadata = SnowAnimationMetadata.from(new DataInputStream(stream));

        executor.submit(() -> consumeSnowFromPipeThread(stream));
        running = true;
    }

    private void consumeSnowFromPipeThread(FileInputStream stream) {
        log.debug("consumeSnowFromPipeThread( {} ) | Start pipe", sessionId);
        try (stream) {
            try (DataInputStream dataStream = new DataInputStream(stream)) {
                while (isActive()) {
                    SnowDataFrame frame = SnowDataFrame.from(dataStream);
                    log.trace("consumeSnowFromPipeThread( {} ) | Frame {}", sessionId, frame.frameNum());
                    buffer.push(frame);
                    if (frame == SnowDataFrame.last) {
                        break;
                    }
                }
                buffer.push(SnowDataFrame.last);
                buffer.destroy();
                running = false;
            }
            log.trace("consumeSnowFromPipeThread( {} ) | Stop pipe", sessionId);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void streamTo(OutputStream out) throws IOException, InterruptedException {
        if (!running) {
            throw new IllegalStateException("You must startPhpApp() first!");
        }

        log.debug("streamTo( {} ) | Reading metadata", sessionId);

        out.write(metadata.toString().getBytes(StandardCharsets.UTF_8));
        out.write("\n\n".getBytes(StandardCharsets.UTF_8));

        log.debug("streamTo( {} ) | Reading first frame", sessionId);
        for (SnowDataFrame frame = buffer.firstFrame(); frame != SnowDataFrame.last; frame = buffer.nextFrame(frame)) {
            if (frame == SnowDataFrame.empty) {
                continue;
            }
            log.trace("streamTo( {} ) | Frame {}", sessionId, frame.frameNum());

            out.write(frame.toString().getBytes(StandardCharsets.UTF_8));
            out.write("\n\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    public void ensureCompatibleWithConfig(Map<String, String> config) {
        if (config == null || config.isEmpty()) {
            return;
        }
        if (!configProvider.getObject(config).equals(phpSnowConfig)) {
            throw new IllegalArgumentException("You cannot change config when animation is running.");
        }
    }
}