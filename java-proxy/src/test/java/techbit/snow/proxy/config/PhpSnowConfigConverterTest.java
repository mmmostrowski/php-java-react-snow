package techbit.snow.proxy.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhpSnowConfigConverterTest {

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<PhpSnowConfig> issue;

    private PhpSnowConfigConverter converter;

    @BeforeEach
    void setup() {
        converter = new PhpSnowConfigConverter(
                "somePreset", 101, 53, Duration.ofMinutes(3), 23, validator);
    }

    @Test
    void givenEmptyConfigMap_whenConvertFromMap_thenReturnsConfigObjectWithDefaults() {
        PhpSnowConfig config = converter.fromMap(Map.of());

        assertEquals(Duration.ofMinutes(3), config.duration());
        assertEquals("somePreset", config.presetName());
        assertEquals(101, config.width());
        assertEquals(53, config.height());
        assertEquals(23, config.fps());
    }

    @Test
    void givenPresetName_whenConvertFromMap_thenReturnsValidConfigObject() {
        PhpSnowConfig config = converter.fromMap(Map.of("presetName", "redefinedPresetName"));

        assertEquals("redefinedPresetName", config.presetName());
    }

    @Test
    void givenFps_whenConvertFromMap_thenReturnsValidConfigObject() {
        PhpSnowConfig config = converter.fromMap(Map.of("fps", "13"));

        assertEquals(13, config.fps());
    }

    @Test
    void givenWidth_whenConvertFromMap_thenReturnsValidConfigObject() {
        PhpSnowConfig config = converter.fromMap(Map.of("width", "113"));

        assertEquals(113, config.width());
    }

    @Test
    void givenHeight_whenConvertFromMap_thenReturnsValidConfigObject() {
        PhpSnowConfig config = converter.fromMap(Map.of("height", "73"));

        assertEquals(73, config.height());
    }

    @Test
    void givenDuration_whenConvertFromMap_thenReturnsValidConfigObject() {
        PhpSnowConfig config = converter.fromMap(Map.of("duration", "30"));

        assertEquals(Duration.ofSeconds(30), config.duration());
    }

    @Test
    void givenInvalidFps_whenConvertFromMap_thenThrowException() {
        when(validator.validate(any(PhpSnowConfig.class))).thenReturn(Set.of(issue));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("fps", "0")));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("fps", "-1")));
    }

    @Test
    void givenInvalidWidth_whenConvertFromMap_thenThrowException() {
        when(validator.validate(any(PhpSnowConfig.class))).thenReturn(Set.of(issue));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("width", "0")));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("width", "-1")));
    }

    @Test
    void givenInvalidHeight_whenConvertFromMap_thenThrowException() {
        when(validator.validate(any(PhpSnowConfig.class))).thenReturn(Set.of(issue));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("height", "0")));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("height", "-1")));
    }

    @Test
    void givenInvalidDuration_whenConvertFromMap_thenThrowException() {
        when(validator.validate(any(PhpSnowConfig.class))).thenReturn(Set.of(issue));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("duration", "0")));
        assertThrows(ConstraintViolationException.class, () -> converter.fromMap(Map.of("duration", "-1")));
    }

    @Test
    void givenValidConfigMap_whenConvertFromMap_thenReturnsConfigObject() {
        PhpSnowConfig config = converter.fromMap(Map.of(
                "presetName", "redefinedPresetName",
                "fps", "13",
                "width", "113",
                "height", "73",
                "duration", "5940"
        ));

        assertEquals("redefinedPresetName", config.presetName());
        assertEquals(113, config.width());
        assertEquals(73, config.height());
        assertEquals(Duration.ofMinutes(99), config.duration());
        assertEquals(13, config.fps());
    }

    @Test
    void givenConfigObject_whenConvertToMap_thenReturnsProperMap() {
        PhpSnowConfig config = new PhpSnowConfig(
                "preset-name", "BASE64BASE64=", 999, 888, Duration.ofDays(1), 66);

        Map<String, Object> map = converter.toMap(config);

        assertEquals("preset-name", map.get("presetName"));
        assertEquals("BASE64BASE64=", map.get("scene"));
        assertEquals(999, map.get("width"));
        assertEquals(888, map.get("height"));
        assertEquals(66, map.get("fps"));
        assertEquals(86400L, ((BigDecimal) map.get("duration")).longValue());
    }


}