/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 */
package com.netflix.conductor.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.conductor.aurora.*;
import com.netflix.conductor.contribs.*;
import com.netflix.conductor.contribs.json.JsonJqTransform;
import com.netflix.conductor.contribs.validation.ValidationTask;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.config.CoreModule;
import com.netflix.conductor.core.execution.TaskStatusListener;
import com.netflix.conductor.core.execution.WorkflowStatusListener;
import com.netflix.conductor.core.execution.WorkflowSweeper;
import com.netflix.conductor.core.utils.PriorityLookup;
import com.netflix.conductor.dao.*;
import com.netflix.conductor.dao.dynomite.DynoProxy;
import com.netflix.conductor.dao.dynomite.RedisExecutionDAO;
import com.netflix.conductor.dao.dynomite.RedisMetadataDAO;
import com.netflix.conductor.dao.dynomite.queue.DynoQueueDAO;
import com.netflix.conductor.dao.es6rest.Elasticsearch6RestModule;
import com.netflix.conductor.dao.es6rest.dao.*;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.queues.redis.DynoShardSupplier;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import redis.clients.jedis.JedisCommands;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Viren
 *
 */
public class ServerModule extends AbstractModule {

	private int maxThreads = 50;

	private ExecutorService es;

	private JedisCommands dynoConn;

	private HostSupplier hs;

	private String region;

	private String localRack;

	private ConductorConfig config;

	private ConductorServer.DB db;

	public ServerModule(JedisCommands jedis, HostSupplier hs, ConductorConfig config, ConductorServer.DB db) {
		this.dynoConn = jedis;
		this.hs = hs;
		this.config = config;
		this.region = config.getRegion();
		this.localRack = config.getAvailabilityZone();
		this.db = db;
	}

	@Override
	protected void configure() {
		Registry registry = new DefaultRegistry();
		Spectator.globalRegistry().add(registry);

		configureExecutorService();
		bind(Configuration.class).toInstance(config);
		bind(Registry.class).toInstance(registry);

		if (ConductorServer.DB.elasticsearch.equals(db)) {
			install(new Elasticsearch6RestModule());

			bind(ExecutionDAO.class).to(Elasticsearch6RestExecutionDAO.class);
			bind(MetadataDAO.class).to(Elasticsearch6RestMetadataDAO.class);
			bind(QueueDAO.class).to(Elasticsearch6RestQueueDAO.class);
			bind(MetricsDAO.class).to(Elasticsearch6RestMetricsDAO.class);
			bind(IndexDAO.class).to(Elasticsearch6RestIndexDAO.class);
			bind(ErrorLookupDAO.class).to(Elasticsearch6ErrorLookupDAO.class);
			bind(PriorityLookupDAO.class).to(ElasticSearch6PriorityLookupDAO.class);
		} else if (ConductorServer.DB.aurora.equals(db)) {
			install(new AuroraModule());

			bind(ExecutionDAO.class).to(AuroraExecutionDAO.class).asEagerSingleton();;
			bind(MetadataDAO.class).to(AuroraMetadataDAO.class).asEagerSingleton();;
			bind(QueueDAO.class).to(AuroraQueueDAO.class).asEagerSingleton();;
			bind(MetricsDAO.class).to(AuroraMetricsDAO.class).asEagerSingleton();;
			bind(IndexDAO.class).to(AuroraIndexDAO.class).asEagerSingleton();;
			bind(ErrorLookupDAO.class).to(AuroraErrorLookupDAO.class).asEagerSingleton();;
			bind(PriorityLookupDAO.class).to(AuroraPriorityLookupDAO.class).asEagerSingleton();;
		} else {
			String localDC = localRack.replaceAll(region, "");
			DynoShardSupplier ss = new DynoShardSupplier(hs, region, localDC);
			DynoQueueDAO queueDao = new DynoQueueDAO(dynoConn, dynoConn, ss, config);

			DynoProxy proxy = new DynoProxy(dynoConn);
			bind(DynoProxy.class).toInstance(proxy);

			bind(ExecutionDAO.class).to(RedisExecutionDAO.class);
			bind(MetadataDAO.class).to(RedisMetadataDAO.class);
			bind(DynoQueueDAO.class).toInstance(queueDao);
			bind(QueueDAO.class).to(DynoQueueDAO.class);
		}

		List<AbstractModule> additionalModules = config.getAdditionalModules();
		if (additionalModules != null) {
			for (AbstractModule additionalModule : additionalModules) {
				install(additionalModule);
			}
		}
		install(new CoreModule());
		install(new JerseyModule());
		install(new HttpModule());
		install(new AuthModule());
		install(new AssetModule());
		install(new ProgressModule());
		install(new StatusModule());
		install(new TaskUpdateModule());
		new JsonJqTransform();
		new ValidationTask();
		bind(TaskStatusListener.class).to(StatusEventPublisher.class).asEagerSingleton();
		bind(WorkflowStatusListener.class).to(StatusEventPublisher.class).asEagerSingleton();
		bind(ServerShutdown.class).asEagerSingleton();
	}

	@Provides
	public ExecutorService getExecutorService() {
		return this.es;
	}

	private void configureExecutorService() {
		AtomicInteger count = new AtomicInteger(0);
		this.es = java.util.concurrent.Executors.newFixedThreadPool(maxThreads, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("conductor-worker-" + count.getAndIncrement());
				return t;
			}
		});
	}
}
