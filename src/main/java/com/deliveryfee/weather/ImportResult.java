package com.deliveryfee.weather;

import java.util.List;

/**
 * Summary of a single weather import run.
 *
 * @param imported number of new observations persisted
 * @param skipped number of feed entries ignored (already stored or missing required data)
 * @param missingStations relevant stations absent from the feed
 */
public record ImportResult(int imported, int skipped, List<String> missingStations) {}
