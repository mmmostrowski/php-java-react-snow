package techbit.snow.proxy.service.stream.encoding;

import org.springframework.stereotype.Component;
import techbit.snow.proxy.dto.SnowAnimationBackground;
import techbit.snow.proxy.dto.SnowAnimationBasis;
import techbit.snow.proxy.dto.SnowAnimationMetadata;
import techbit.snow.proxy.dto.SnowDataFrame;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Component
public class BinaryStreamEncoder implements StreamEncoder {

    @Override
    public void encodeMetadata(SnowAnimationMetadata metadata, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(metadata.width());
        data.writeInt(metadata.height());
        data.writeInt(metadata.fps());
    }

    @Override
    public void encodeBackground(SnowAnimationBackground background, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(background.width());
        if (background.width() > 0) {
            data.writeInt(background.height());

            final byte[][] pixels = background.pixels();
            for (int x = 0; x < background.width(); ++x) {
                out.write(pixels[x]);
            }
        }
    }

    @Override
    public void encodeFrame(SnowDataFrame frame, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(frame.frameNum());
        data.writeInt(frame.chunkSize());

        for (int i = 0; i < frame.chunkSize(); ++i) {
            data.writeFloat(frame.x(i));
            data.writeFloat(frame.y(i));
            data.writeByte(frame.flakeShape(i));
        }
    }

    @Override
    public void encodeBasis(SnowAnimationBasis basis, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(basis.numOfPixels());
        for (int i = 0; i < basis.numOfPixels(); ++i) {
            data.writeInt(basis.x(i));
        }
        for (int i = 0; i < basis.numOfPixels(); ++i) {
            data.writeInt(basis.y(i));
        }
        out.write(basis.pixels());
    }

}
