package com.deliveryfee.weather.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * A single station entry in the observations feed.
 *
 * <p>Numeric values are kept as strings so that blank/empty elements become {@code null} through
 * the mapper's blank-to-null deserializer and are parsed defensively by the import service.
 */
@JacksonXmlRootElement(localName = "station")
public record StationXml(
        @JacksonXmlProperty(localName = "name") String name,
        @JacksonXmlProperty(localName = "wmocode") String wmoCode,
        @JacksonXmlProperty(localName = "airtemperature") String airTemperature,
        @JacksonXmlProperty(localName = "windspeed") String windSpeed,
        @JacksonXmlProperty(localName = "phenomenon") String phenomenon) {}
