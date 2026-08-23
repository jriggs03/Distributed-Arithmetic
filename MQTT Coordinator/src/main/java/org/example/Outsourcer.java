package org.example;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Outsourcer {

    private final OperationRepository repository;
    private final MqttPublisher publisher;
    private final NoteTaker noteTaker;
    private final ConsoleView view;
    private final long timeoutMillis;

    private final ExecutorService assignmentExecutor;
    private final ScheduledExecutorService timeoutExecutor;

    public Outsourcer(OperationRepository repository, MqttPublisher publisher, NoteTaker noteTaker, ConsoleView view, long timeoutMillis) {
        this.repository = repository;
        this.publisher = publisher;
        this.noteTaker = noteTaker;
        this.view = view;
        this.timeoutMillis = timeoutMillis;

        assignmentExecutor = Executors.newCachedThreadPool();
        timeoutExecutor = Executors.newScheduledThreadPool(2);
    }

    public void workerAvailable(String workerId) {
        view.workerAvailable(workerId);

        assignmentExecutor.execute(
                () -> assignOperation(workerId));
    }

    private void assignOperation(String workerId) {
        try {
            Operation operation = repository.removeOperation();
            ScheduledFuture<?> timeoutTask = timeoutExecutor.schedule(
                            () -> recoverOperation(operation.getId()), timeoutMillis, TimeUnit.MILLISECONDS);

            PendingJob pendingJob = new PendingJob(workerId, operation, timeoutTask);

            noteTaker.rememberJob(pendingJob);

            try {
                String topic = Protocol.JOB_TOPIC_PREFIX + workerId;
                String message = Protocol.encodeJob(operation);

                publisher.publishMessage(topic, message);
                view.assigned(workerId, operation);

            } catch (MqttException exception) {
                handlePublishFailure(operation, workerId, exception);
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void handlePublishFailure(Operation operation, String workerId, MqttException exception) {
        PendingJob pendingJob = noteTaker.removeJob(operation.getId());

        if (pendingJob != null) {
            pendingJob.getTimeout().cancel(false);
            try {
                repository.addOperation(operation);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        view.info("Could not assign operation to " + workerId + ": " + exception.getMessage());
    }

    public void resultReceived(Protocol.ResultMessage result) {
        PendingJob pendingJob = noteTaker.removeJob(result.getJobId());

        if (pendingJob == null) {
            view.info("Ignored late or unknown result for " + result.getJobId() + " from " + result.getWorkerId());
            return;
        }

        pendingJob.getTimeout().cancel(false);

        if (!pendingJob.getWorkerId().equals(result.getWorkerId())) {
            view.info("Ignored result because job was assigned to " + pendingJob.getWorkerId());

            recoverJob(pendingJob);
            return;
        }

        view.remoteResult(result.getWorkerId(), pendingJob.getOperation(), result.getResult());
    }

    private void recoverOperation(String operationId) {
        PendingJob pendingJob = noteTaker.removeJob(operationId);

        if (pendingJob != null) {
            recoverJob(pendingJob);
        }
    }

    private void recoverJob(PendingJob pendingJob) {
        try {
            repository.addOperation(pendingJob.getOperation());
            view.recovered(pendingJob.getWorkerId(), pendingJob.getOperation());

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        assignmentExecutor.shutdownNow();
        timeoutExecutor.shutdownNow();
    }
}