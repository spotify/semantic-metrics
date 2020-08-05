package com.spotify.metrics.resources;

import java.util.Map;

public class NoopResourceTagExtractor implements ResourceExtractor {
    /**
     * Noop does nothing.
     *
     * @return an unmodified map.
     */
    @Override
    public Map<String, String> addResources(Map<String, String> resources) {
        return resources;
    }

}
