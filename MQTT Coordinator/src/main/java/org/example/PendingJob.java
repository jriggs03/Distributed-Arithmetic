package org.example;
import java.util.concurrent.ScheduledFuture;

public class PendingJob {

    private final String workerId;
    private final Operation operation;
    private final ScheduledFuture<?> timeout;

    public PendingJob(String workerId, Operation operation, ScheduledFuture<?> timeout) {
        this.workerId = workerId;
        this.operation = operation;
        this.timeout = timeout;
    }
    public String getWorkerId() {
        return workerId;
    }
    public Operation getOperation() {
        return operation;
    }
    public ScheduledFuture<?> getTimeout() {
        return timeout;
    }
}
