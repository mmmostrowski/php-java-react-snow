package techbit.snow.proxy.dto;

import java.util.Arrays;

public record SnowBasis(
        int numOfPixels,
        int[] x,
        int[] y,
        byte[] pixels
) {
    public static final SnowBasis NONE = new SnowBasis(
            0, new int[]{}, new int[]{}, new byte[]{});

    public int x(int i) {
        return x[i];
    }

    public int y(int i) {
        return y[i];
    }

    public byte pixel(int i) {
        return pixels[i];
    }

    @Override
    public String toString() {
        if (this == NONE) {
            return "Basis.NONE";
        }
        return "Basis{" +
                "numOfPixels=" + numOfPixels +
                ",\n    particlesX=" + Arrays.toString(x) +
                ",\n    particlesY=" + Arrays.toString(y) +
                ",\n    pixels=" + Arrays.toString(pixels) +
                '}';
    }
}
