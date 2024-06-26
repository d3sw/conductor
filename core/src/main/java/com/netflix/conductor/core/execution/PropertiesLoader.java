package com.netflix.conductor.core.execution;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.conductor.dao.MetadataDAO;
import org.apache.commons.lang.text.StrSubstitutor;

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
        metadata.getConfigs()
                .forEach(it -> properties.put(it.getLeft(), StrSubstitutor.replace(it.getRight(), System.getenv())));
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
