package com.deliveryfee.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PhenomenonCategoryTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("classificationCases")
    void classify_mapsPhenomenonToExpectedCategory(String phenomenon, PhenomenonCategory expected) {
        assertThat(PhenomenonCategory.classify(phenomenon)).isEqualTo(expected);
    }

    private static Stream<Arguments> classificationCases() {
        return Stream.of(
                Arguments.of("Light snow shower", PhenomenonCategory.SNOW_SLEET),
                Arguments.of("snow", PhenomenonCategory.SNOW_SLEET),
                Arguments.of("Sleet", PhenomenonCategory.SNOW_SLEET),
                Arguments.of("rain shower", PhenomenonCategory.RAIN),
                Arguments.of("Moderate drizzle", PhenomenonCategory.RAIN),
                Arguments.of("heavy RAIN", PhenomenonCategory.RAIN),
                Arguments.of("glaze", PhenomenonCategory.GLAZE_HAIL_THUNDER),
                Arguments.of("hail", PhenomenonCategory.GLAZE_HAIL_THUNDER),
                Arguments.of("thunderstorm", PhenomenonCategory.GLAZE_HAIL_THUNDER),
                Arguments.of("Cloudy with clear spells", null),
                Arguments.of("", null),
                Arguments.of(null, null));
    }
}
