package com.netflix.conductor.aurora;

import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.dao.AppConfigDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class AuroraAppConfigDAO extends AuroraBaseDAO implements AppConfigDAO {
    @Inject
    public AuroraAppConfigDAO(DataSource dataSource, ObjectMapper mapper, Configuration config) {
        super(dataSource, mapper);
    }

    public Pair<String, String> getConfigsByName(String key){
        final String SQL = "SELECT * FROM app_config where key = ?";
        return queryWithTransaction(SQL, q -> q.addParameter(key)
                .executeAndFetchFirst(Pair.class));

    }

    public Map<String, String> getConfigs(){
        final String SQL = "SELECT key, value FROM app_config ORDER BY key, value";
        return queryWithTransaction(SQL, q -> q.executeAndFetch(rs -> {
            Map<String, String> configs = new HashMap<>();
            while (rs.next()) {
                configs.put(rs.getString(1), rs.getString(2));
            }
            return configs;
        }));
    }

    public void setAppConfigValue(String key, String value){
        final String SQL = "UPDATE app_config set value = ? where key = ?";
        queryWithTransaction(SQL, q -> q.addParameter(value)
                .addParameter(key)
                .executeUpdate());
    }

    public void removeAppConfigValue(String key, String value){
        final String SQL = "DELETE FROM app_config where key = ?";
        queryWithTransaction(SQL, q -> q.addParameter(key).executeUpdate());
    }




}
