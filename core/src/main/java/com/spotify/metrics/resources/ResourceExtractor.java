package com.spotify.metrics.resources;

import java.util.Map;

/**
 * Extract resources to be added to and enrich Metrics.
 */
public interface ResourceExtractor {

    /**
     * Creates a new map with the extracted resources from the supplied map.
     *
     * @return map with extracted resources added.
     */
    Map<String, String> addResources(Map<String, String> resources);

}
