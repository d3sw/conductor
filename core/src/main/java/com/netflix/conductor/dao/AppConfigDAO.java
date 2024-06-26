package com.netflix.conductor.dao;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Map;

public interface AppConfigDAO {
    public default Pair<String, String> getConfigsByName(String name){
        return null;
    }

    public default Map<String, String> getConfigs(){
        return Collections.emptyMap();
    }

    public default void setAppConfigValue(String key, String value){}

    public default void removeAppConfigValue(String key){}
}


