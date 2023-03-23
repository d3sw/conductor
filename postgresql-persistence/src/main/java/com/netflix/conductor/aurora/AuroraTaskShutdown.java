package com.netflix.conductor.aurora;

public interface AuroraTaskShutdown {

    /**
     * Shuts down active aurora execution task
     */
    void shutdown();

    /**
     * Checks if this task is still in progress after a shutdown instruction
     * Helps stall dependent services from shutting down/closing before completion
     * @return the state of the executor task
     */
    boolean isTaskTerminated();
}
