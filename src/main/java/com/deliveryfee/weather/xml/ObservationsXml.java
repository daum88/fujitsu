package com.deliveryfee.weather.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 * Root of the Ilmateenistus observations feed.
 *
 * <p>Unknown fields are ignored by the hardened mapper; only the timestamp and the stations are
 * bound.
 */
@JacksonXmlRootElement(localName = "observations")
public record ObservationsXml(
        @JacksonXmlProperty(isAttribute = true, localName = "timestamp") long timestamp,
        @JacksonXmlProperty(localName = "station") @JacksonXmlElementWrapper(useWrapping = false)
                List<StationXml> stations) {}
