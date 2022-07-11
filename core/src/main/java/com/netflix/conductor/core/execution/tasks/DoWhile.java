/*
 * Copyright 2022 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.execution.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.events.ScriptEvaluator;
import com.netflix.conductor.core.execution.ParametersUtils;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.*;
import java.util.stream.Collectors;

public class DoWhile extends WorkflowSystemTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoWhile.class);

    public static final String TASK_TYPE_DO_WHILE = "DO_WHILE";

    private final ParametersUtils parametersUtils = new ParametersUtils();

    public DoWhile() {
        super(TASK_TYPE_DO_WHILE);
    }

    @Override
    public void cancel(Workflow workflow, Task task, WorkflowExecutor executor) {
        task.setStatus(Task.Status.CANCELED);
    }

    @Override
    public boolean execute(
            Workflow workflow, Task doWhileTaskModel, WorkflowExecutor workflowExecutor) {

        boolean allDone = true;
        boolean hasFailures = false;
        StringBuilder failureReason = new StringBuilder();
        Map<String, Object> output = new HashMap<>();

        /*
         * Get the latest set of tasks (the ones that have the highest retry count). We don't want to evaluate any tasks
         * that have already failed if there is a more current one (a later retry count).
         */
        Map<String, Task> relevantTasks = new LinkedHashMap<>();
        Task relevantTask;
        for (Task t : workflow.getTasks()) {
            if (doWhileTaskModel
                            .getWorkflowTask()
                            .has(TaskUtils.removeIterationFromTaskRefName(t.getReferenceTaskName()))
                    && !doWhileTaskModel.getReferenceTaskName().equals(t.getReferenceTaskName())
                    && doWhileTaskModel.getIteration() == t.getIteration()) {
                relevantTask = relevantTasks.get(t.getReferenceTaskName());
                if (relevantTask == null || t.getRetryCount() > relevantTask.getRetryCount()) {
                    relevantTasks.put(t.getReferenceTaskName(), t);
                }
            }
        }
        Collection<Task> loopOverTasks = relevantTasks.values();
        LOGGER.info(
                "Workflow {} waiting for tasks {} to complete iteration {}",
                workflow.getWorkflowId(),
                loopOverTasks.stream()
                        .map(Task::getReferenceTaskName)
                        .collect(Collectors.toList()),
                doWhileTaskModel.getIteration());

        // if the loopOver collection is empty, no tasks inside the loop have been scheduled.
        // so schedule it and exit the method.
        if (loopOverTasks.isEmpty()) {
            doWhileTaskModel.setIteration(1);
            doWhileTaskModel.addOutput("iteration", doWhileTaskModel.getIteration());
            return scheduleNextIteration(doWhileTaskModel, workflow, workflowExecutor);
        }

        for (Task loopOverTask : loopOverTasks) {
            Task.Status taskStatus = loopOverTask.getStatus();
            hasFailures = !taskStatus.isSuccessful();
            if (hasFailures) {
                failureReason.append(loopOverTask.getReasonForIncompletion()).append(" ");
            }
            output.put(
                    TaskUtils.removeIterationFromTaskRefName(loopOverTask.getReferenceTaskName()),
                    loopOverTask.getOutputData());
            allDone = taskStatus.isTerminal();
            if (!allDone || hasFailures) {
                break;
            }
        }
        doWhileTaskModel
                .getOutputData()
                .put(String.valueOf(doWhileTaskModel.getIteration()), output);
        if (hasFailures) {
            LOGGER.debug(
                    "Task {} failed in {} iteration",
                    doWhileTaskModel.getTaskId(),
                    doWhileTaskModel.getIteration() + 1);
            return updateLoopTask(
                    doWhileTaskModel, Task.Status.FAILED, failureReason.toString());
        } else if (!allDone) {
            return false;
        }
        boolean shouldContinue;
        try {
            shouldContinue = getEvaluatedCondition(workflow, doWhileTaskModel, workflowExecutor);
            LOGGER.debug(
                    "Task {} condition evaluated to {}",
                    doWhileTaskModel.getTaskId(),
                    shouldContinue);
            if (shouldContinue) {
                doWhileTaskModel.setIteration(doWhileTaskModel.getIteration() + 1);
                doWhileTaskModel.getOutputData().put("iteration", doWhileTaskModel.getIteration());
                return scheduleNextIteration(doWhileTaskModel, workflow, workflowExecutor);
            } else {
                LOGGER.debug(
                        "Task {} took {} iterations to complete",
                        doWhileTaskModel.getTaskId(),
                        doWhileTaskModel.getIteration() + 1);
                return markLoopTaskSuccess(doWhileTaskModel);
            }
        } catch (ScriptException e) {
            String message =
                    String.format(
                            "Unable to evaluate condition %s , exception %s",
                            doWhileTaskModel.getWorkflowTask().getLoopCondition(), e.getMessage());
            LOGGER.error(message);
            LOGGER.error("Marking task {} failed with error.", doWhileTaskModel.getTaskId());
            return updateLoopTask(
                    doWhileTaskModel, Task.Status.FAILED, message);
        }
    }

    boolean scheduleNextIteration(
            Task task, Workflow workflow, WorkflowExecutor workflowExecutor) {
        LOGGER.info(
                "Scheduling loop tasks for task {} as condition {} evaluated to true",
                task.getTaskId(),
                task.getWorkflowTask().getLoopCondition());
        workflowExecutor.scheduleNextIteration(task, workflow);
        return true; // Return true even though status not changed. Iteration has to be updated in
        // execution DAO.
    }

    boolean updateLoopTask(Task task, Task.Status status, String failureReason) {
        task.setReasonForIncompletion(failureReason);
        task.setStatus(status);
        return true;
    }

    boolean markLoopTaskSuccess(Task task) {
        LOGGER.debug(
                "task {} took {} iterations to complete",
                task.getTaskId(),
                task.getIteration() + 1);
        task.setStatus(Task.Status.COMPLETED);
        return true;
    }

    @VisibleForTesting
    boolean getEvaluatedCondition(
            Workflow workflow, Task task, WorkflowExecutor workflowExecutor)
            throws ScriptException {
        TaskDef taskDefinition = null;
        try {
            taskDefinition = workflowExecutor.getTaskDefinition(task);
        } catch (Exception e) {
            // It is ok to not have a task definition for a DO_WHILE task
        }

        Map<String, Object> taskInput =
                parametersUtils.getTaskInputV2(
                        task.getWorkflowTask().getInputParameters(),
                        workflow,
                        task.getTaskId(),
                        taskDefinition);
        taskInput.put(task.getReferenceTaskName(), task.getOutputData());
        List<Task> loopOver =
                workflow.getTasks().stream()
                        .filter(
                                t ->
                                        (task.getWorkflowTask()
                                                        .has(
                                                                TaskUtils
                                                                        .removeIterationFromTaskRefName(
                                                                                t
                                                                                        .getReferenceTaskName()))
                                                && !task.getReferenceTaskName()
                                                        .equals(t.getReferenceTaskName())))
                        .collect(Collectors.toList());

        for (Task loopOverTask : loopOver) {
            taskInput.put(
                    TaskUtils.removeIterationFromTaskRefName(loopOverTask.getReferenceTaskName()),
                    loopOverTask.getOutputData());
        }
        String condition = task.getWorkflowTask().getLoopCondition();
        boolean shouldContinue = false;
        if (condition != null) {
            LOGGER.debug("Condition: {} is being evaluated", condition);
            // Evaluate the expression by using the Nashorn based script evaluator
            shouldContinue = ScriptEvaluator.evalBool(condition, taskInput);
        }
        return shouldContinue;
    }
}
