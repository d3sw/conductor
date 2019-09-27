package com.netflix.conductor.server;

import org.eclipse.jetty.server.AbstractNCSARequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLogHandler extends AbstractNCSARequestLog {
	private static Logger logger = LoggerFactory.getLogger(AccessLogHandler.class);

	AccessLogHandler() {
		setLogLatency(true);
	}

	@Override
	protected boolean isEnabled() {
		return true;
	}

	@Override
	public void write(String requestEntry) {
		logger.debug(requestEntry);
	}
}
