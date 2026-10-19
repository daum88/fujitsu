package com.deliveryfee.common;

import java.util.List;
import java.util.Locale;

/**
 * Weather phenomenon categories used by extra fee rules.
 *
 * <p>Categories are checked in declaration order; the first match wins. The order encodes the
 * precedence required by the specification: "Light snow shower" must be classified as snow, not
 * rain, even though it contains the word "shower".
 */
public enum PhenomenonCategory {
    SNOW_SLEET("snow", "sleet"),
    GLAZE_HAIL_THUNDER("glaze", "hail", "thunder", "storm"),
    RAIN("rain", "drizzle", "shower");

    private final List<String> keywords;

    PhenomenonCategory(String... keywords) {
        this.keywords = List.of(keywords);
    }

    /**
     * Checks whether the given phenomenon text contains one of this category's keywords
     * (case-insensitive substring match).
     *
     * @param phenomenon the raw phenomenon description, may be {@code null}
     * @return {@code true} when any keyword is present
     */
    public boolean matches(String phenomenon) {
        if (phenomenon == null) {
            return false;
        }
        var normalized = phenomenon.toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(normalized::contains);
    }

    /**
     * Classifies a phenomenon description into the highest-precedence matching category.
     *
     * @param phenomenon the raw phenomenon description, may be {@code null}
     * @return the first matching category in declaration order, or {@code null} when none match
     */
    public static PhenomenonCategory classify(String phenomenon) {
        if (phenomenon == null || phenomenon.isBlank()) {
            return null;
        }
        for (var category : values()) {
            if (category.matches(phenomenon)) {
                return category;
            }
        }
        return null;
    }
}
