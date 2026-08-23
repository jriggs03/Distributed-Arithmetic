package org.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsoleView {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AtomicInteger generatedCount;
    private final AtomicInteger localCount;
    private final AtomicInteger outsourcedCount;
    private final AtomicInteger remoteCount;
    private final AtomicInteger recoveredCount;

    public ConsoleView() {
        generatedCount = new AtomicInteger();
        localCount = new AtomicInteger();
        outsourcedCount = new AtomicInteger();
        remoteCount = new AtomicInteger();
        recoveredCount = new AtomicInteger();
    }

    public synchronized void generated(Operation operation, int pendingOperations) {
        generatedCount.incrementAndGet();
        print("PRODUCER", operation.getId(), operation.getExpression() + " | queue=" + pendingOperations);
    }

    public synchronized void localResult(String workerName, Operation operation, String result, int pendingOperations) {
        localCount.incrementAndGet();

        print(workerName, operation.getId(), operation.getExpression() + " = " + result + " | local | queue=" + pendingOperations);
    }

    public synchronized void workerAvailable(String workerId) {
        print("REMOTE", "-", workerId + " requested work");
    }

    public synchronized void assigned(String workerId, Operation operation) {
        outsourcedCount.incrementAndGet();
        print("OUTSOURCE", operation.getId(), operation.getExpression() + " -> " + workerId);
    }

    public synchronized void remoteResult(String workerId, Operation operation, String result) {
        remoteCount.incrementAndGet();
        print("RESULT", operation.getId(), operation.getExpression() + " = " + result + " <- " + workerId);
    }

    public synchronized void recovered(String workerId, Operation operation) {
        recoveredCount.incrementAndGet();
        print("TIMEOUT", operation.getId(), "Recovered " + operation.getExpression() + " from " + workerId);
    }

    public synchronized void status(int queuedOperations, int pendingRemoteJobs) {
        String currentTime = LocalTime.now().format(TIME_FORMAT);

        System.out.printf(
                "%s [STATS] generated=%d local=%d outsourced=%d " + "remote=%d recovered=%d queued=%d pendingRemote=%d%n",
                currentTime,
                generatedCount.get(),
                localCount.get(),
                outsourcedCount.get(),
                remoteCount.get(),
                recoveredCount.get(),
                queuedOperations,
                pendingRemoteJobs);
    }

    public synchronized void info(String message) {
        print("SYSTEM", "-", message);
    }

    private void print(String source, String jobId, String message) {
        String shortId = jobId;
        if (jobId.length() > 8) {
            shortId = jobId.substring(0, 8);
        }

        String currentTime = LocalTime.now().format(TIME_FORMAT);
        System.out.printf(
                "%s [%-10s] job=%-8s %s%n",
                currentTime,
                source,
                shortId,
                message
        );
    }
}