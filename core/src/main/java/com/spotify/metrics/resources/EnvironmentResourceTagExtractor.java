package com.spotify.metrics.resources;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.HashMap;
import java.util.Map;

/**
 * Extract resources from the system environment variables.
 *
 */
public class EnvironmentResourceTagExtractor implements ResourceExtractor {

    /**
     * Prefix of environment variable that adds additional resources.
     */
    public static final String FFWD_RESOURCE_PREFIX = "FFWD_RESOURCE_";

    public static Map<String, String> environmentResources;

    public EnvironmentResourceTagExtractor() {
        this(Suppliers.ofInstance(System.getenv()));
    }

    public EnvironmentResourceTagExtractor(Supplier<Map<String, String>> enviromentSupplier) {
        this.environmentResources = filterEnvironmentResources(enviromentSupplier.get());
    }

    /**
     * Extract tags from the system environment variables.
     * Resources extracted from the environment takes precedence and overwrites existing resources
     * with the same key.
     *
     * @return map with extracted resources added.
     */
    @Override
    public Map<String, String> addResources(Map<String, String> resources) {
        final Map<String, String> extractedResources = new HashMap<>(resources);
        extractedResources.putAll(environmentResources);
        return extractedResources;
    }

    /**
     * Extract resources from a map that can correspond to system environment variables.
     *
     * @return extracted resources.
     */
    public static Map<String, String> filterEnvironmentResources(final Map<String, String> env) {
        final Map<String, String> resources = new HashMap<>();

        for (final Map.Entry<String, String> e : env.entrySet()) {
            if (e.getKey().startsWith(FFWD_RESOURCE_PREFIX)) {
                final String tag = e.getKey().substring(FFWD_RESOURCE_PREFIX.length());
                resources.put(tag.toLowerCase(), e.getValue());
            }
        }

        return resources;
    }

}
