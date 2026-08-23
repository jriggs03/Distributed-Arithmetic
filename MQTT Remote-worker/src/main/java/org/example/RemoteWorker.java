package org.example;

import java.time.LocalTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RemoteWorker implements Runnable {

    private final String workerId;
    private final MqttPublisher publisher;
    private final Solver solver;
    private final BlockingQueue<Operation> jobs;
    private final long solveDelayMillis;

    public RemoteWorker(
            String workerId,
            MqttPublisher publisher,
            long solveDelayMillis
    ) {
        this.workerId = workerId;
        this.publisher = publisher;
        this.solveDelayMillis = solveDelayMillis;

        solver = new Solver();
        jobs = new LinkedBlockingQueue<>();
    }

    public void receive(Operation operation)
            throws InterruptedException {

        jobs.put(operation);
    }

    @Override
    public void run() {
        try {
            announceAvailable();
            while (!Thread.currentThread().isInterrupted()) {
                Operation operation = jobs.take();
                log("Received job " + getShortId(operation.getId()) + ": " + operation.getExpression());

                Thread.sleep(solveDelayMillis);

                String result = solver.solve(operation);
                String resultMessage = Protocol.encodeResult(workerId, operation);

                publisher.publishMessage(Protocol.RESULTS_TOPIC, resultMessage);

                log("Completed job " + getShortId(operation.getId()) + ": " + operation.getExpression() + " = " + result);

                announceAvailable();
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

        } catch (Exception exception) {
            log("Worker stopped after error: " + exception.getMessage());
        }
    }

    private void announceAvailable() throws Exception {
        publisher.publishMessage(Protocol.AVAILABLE_TOPIC, workerId);
        log("AVAILABLE - requested work");
    }

    private void log(String message) {
        LocalTime currentTime = LocalTime.now().withNano(0);

        System.out.printf("%s [%s] %s%n", currentTime, workerId, message);
    }

    private String getShortId(String operationId) {
        String shortId = operationId;
        if (operationId.length() > 8) {
            shortId = operationId.substring(0, 8);
        }
        return shortId;
    }
}