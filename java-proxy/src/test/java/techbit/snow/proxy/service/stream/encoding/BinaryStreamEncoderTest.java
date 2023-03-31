package techbit.snow.proxy.service.stream.encoding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import techbit.snow.proxy.dto.SnowAnimationMetadata;
import techbit.snow.proxy.dto.SnowDataFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BinaryStreamEncoderTest {

    private BinaryStreamEncoder encoder;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setup() {
        encoder = new BinaryStreamEncoder();
        out = new ByteArrayOutputStream();
    }


    @Test
    void whenEncodingMetadata_thenBinaryDataAreInOutput() throws IOException {
        SnowAnimationMetadata metadata = new SnowAnimationMetadata(
                99, 101, 15
        );

        encoder.encodeMetadata(metadata, out);

        byte[] expected = new byte[] { 0, 0, 0, 99, 0, 0, 0, 101, 0, 0, 0, 15 };

        assertArrayEquals(expected, out.toByteArray());
    }

    @Test
    void whenEncodingFrame_thenBinaryDataAreInOutput() throws IOException {
        SnowDataFrame frame = new SnowDataFrame(
                78, 2,
                new float[] { 103, 22.5f },
                new float[] { 0.5f, 11f },
                new byte[] { 99, 88 },
                new SnowDataFrame.Background(3, 2, new byte[][]{
                        new byte[] { 1, 2 },
                        new byte[] { 3, 4 },
                        new byte[] { 5, 6 },
                }),
                new SnowDataFrame.Basis(4,
                        new int[] { 1, 2, 3, 4 },
                        new int[] { 11, 12, 13, 14 },
                        new byte[] { 8, 7, 6, 5 }
                )
        );

        encoder.encodeFrame(frame, out);

        byte[] expected = new byte[] {
                0, 0, 0, 78,
                0, 0, 0, 2,

                66, -50, 0, 0,
                63, 0, 0, 0,
                99,

                65, -76, 0, 0,
                65, 48, 0, 0,
                88
        };

        assertArrayEquals(expected, out.toByteArray());
    }

}