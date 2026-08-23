package org.example;

public class LocalWorker implements Runnable {

    private final String workerName;
    private final OperationRepository repository;
    private final ConsoleView view;
    private final long solveDelayMillis;

    public LocalWorker(String workerName, OperationRepository repository, ConsoleView view, long solveDelayMillis) {
        this.workerName = workerName;
        this.repository = repository;
        this.view = view;
        this.solveDelayMillis = solveDelayMillis;
    }
    @Override
    public void run() {

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Operation operation = repository.removeOperation();
                Thread.sleep(solveDelayMillis);
                String result = operation.getResultText();
                view.localResult(workerName, operation, result, repository.getSize());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
