package techbit.snow.proxy.snow.transcoding;

import techbit.snow.proxy.dto.SnowAnimationMetadata;
import techbit.snow.proxy.dto.SnowBackground;
import techbit.snow.proxy.dto.SnowBasis;
import techbit.snow.proxy.dto.SnowDataFrame;

import java.io.IOException;
import java.io.OutputStream;

public interface StreamEncoder {

    void encodeMetadata(SnowAnimationMetadata metadata, OutputStream out) throws IOException;

    void encodeBackground(SnowBackground background, OutputStream out) throws IOException;

    void encodeFrame(SnowDataFrame frame, OutputStream out) throws IOException;

    void encodeBasis(SnowBasis basis, OutputStream out) throws IOException;

}
