package com.netflix.conductor.common.run;

import java.util.List;

public class WorkflowCancelSummary {

    private List<String> completed;

    private List<String> skipped;

    public WorkflowCancelSummary() {

    }

    public WorkflowCancelSummary(List<String> completed, List<String> skipped) {
        this.completed = completed;
        this.skipped = skipped;
    }

    public List<String> getCompleted() {
        return completed;
    }

    public void setCompleted(List<String> completed) {
        this.completed = completed;
    }

    public List<String> getSkipped() {
        return skipped;
    }

    public void setSkipped(List<String> skipped) {
        this.skipped = skipped;
    }

}
