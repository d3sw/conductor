package com.netflix.conductor.aurora;

import com.netflix.conductor.core.config.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class AuroraDataSourceProvider implements Provider<HikariDataSource> {
	private final Configuration config;

	@Inject
	public AuroraDataSourceProvider(Configuration config) {
		this.config = config;
	}

	@Override
	public HikariDataSource get() {
		String db = config.getProperty("aurora.db", null);
		String host = config.getProperty("aurora.host", null);
		String port = config.getProperty("aurora.port", "5432");
		String user = config.getProperty("aurora.user", null);
		String pwd = config.getProperty("aurora.password", null);

		String options = config.getProperty("aurora.options", "");
		String url = String.format("jdbc:postgresql://%s:%s/%s?%s", host, port, db, options);

		String pool_name = config.getProperty("aurora.app.pool.name", "worker");

		HikariConfig poolConfig = new HikariConfig();
		poolConfig.setJdbcUrl(url);
		poolConfig.setUsername(user);
		poolConfig.setPassword(pwd);
		poolConfig.setAutoCommit(false);
		poolConfig.setPoolName(pool_name);
		poolConfig.addDataSourceProperty("ApplicationName", pool_name + "-" + getHostName());
		poolConfig.addDataSourceProperty("cachePrepStmts", "true");
		poolConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		poolConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		poolConfig.setMaximumPoolSize(config.getIntProperty("aurora." + pool_name + ".pool.size", 10));
		poolConfig.setConnectionTimeout(config.getIntProperty("aurora." + pool_name + ".connect.timeout", 30) * 1000L);
		poolConfig.setLeakDetectionThreshold(config.getIntProperty("aurora." + pool_name + ".leakDetection.timeout", 0) * 1000L);

		return new HikariDataSource(poolConfig);
	}

	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "unknown";
		}
	}
}
