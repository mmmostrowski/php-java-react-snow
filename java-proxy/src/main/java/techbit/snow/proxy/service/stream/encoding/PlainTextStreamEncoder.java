package techbit.snow.proxy.service.stream.encoding;

import org.springframework.stereotype.Component;
import techbit.snow.proxy.dto.SnowAnimationMetadata;
import techbit.snow.proxy.dto.SnowDataFrame;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Component(PlainTextStreamEncoder.ENCODER_NAME)
public class PlainTextStreamEncoder implements StreamEncoder {

    public static final String ENCODER_NAME = "PLAIN_TEXT_ENCODER";

    private final byte[] separator = "\n\n".getBytes(StandardCharsets.UTF_8);

    @Override
    public void encodeMetadata(SnowAnimationMetadata metadata, OutputStream out) throws IOException {
        out.write(metadata.toString().getBytes(StandardCharsets.UTF_8));
        out.write(separator);
    }

    @Override
    public void encodeFrame(SnowDataFrame frame, OutputStream out) throws IOException {
        out.write(frame.toString().getBytes(StandardCharsets.UTF_8));
        out.write(separator);
    }
}
