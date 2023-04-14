package techbit.snow.proxy.dto;

import java.util.Arrays;
import java.util.stream.Collectors;

public record SnowBackground(int width, int height, byte[][] pixels) {
    public static final SnowBackground NONE = new SnowBackground(0, 0, new byte[][] {});

    @Override
    public String toString() {
        if (this == NONE) {
            return "Background.NONE";
        }
        final String pixels = Arrays.stream(this.pixels)
                .map(Arrays::toString)
                .collect(Collectors.joining("\n    "));
        return "Background{" +
                "width=" + width +
                ", height=" + height +
                ", pixels=\n    " + pixels +
                "}\n\n";
    }
}