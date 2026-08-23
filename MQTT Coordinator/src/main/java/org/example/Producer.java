package org.example;

import java.util.Random;

public class Producer implements Runnable {

    private final OperationRepository repository;
    private final ConsoleView view;
    private final long delayMillis;
    private final Random random;

    public Producer(OperationRepository repository, ConsoleView view, long delayMillis) {
        this.repository = repository;
        this.view = view;
        this.delayMillis = delayMillis;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Operation operation = Operation.createRandomOperation(random);
                repository.addOperation(operation);
                view.generated(operation, repository.getSize());

                Thread.sleep(delayMillis);
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}