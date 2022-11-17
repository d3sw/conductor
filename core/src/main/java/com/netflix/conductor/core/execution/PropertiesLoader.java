package com.netflix.conductor.core.execution;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.conductor.dao.MetadataDAO;

import java.util.Map;

@Singleton
public class PropertiesLoader {
    private final MetadataDAO metadata;
    private final Map<String, String> properties;


    @Inject
    public PropertiesLoader(MetadataDAO metadata, @Named("properties") Map<String, String> properties) {
        this.metadata = metadata;
        this.properties = properties;
    }

    @Inject
    public void init() {
        metadata.getConfigsByIsPreloaded(true).forEach(it -> properties.put(it.getLeft(), it.getRight()));
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
