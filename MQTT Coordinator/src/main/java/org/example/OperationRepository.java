package org.example;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class OperationRepository {

    private final Queue<Operation> operations;
    private final Semaphore emptySpaces;
    private final Semaphore availableOperations;
    private final ReentrantLock lock;

    public OperationRepository(int capacity) {
        operations = new ArrayDeque<>();
        emptySpaces = new Semaphore(capacity);
        availableOperations = new Semaphore(0);
        lock = new ReentrantLock();
    }

    public void addOperation(Operation operation) throws InterruptedException {
        emptySpaces.acquire();
        lock.lock();

        try {
            operations.add(operation);
        } finally {
            lock.unlock();
        }
        availableOperations.release();
    }

    public Operation removeOperation() throws InterruptedException {
        availableOperations.acquire();
        Operation operation;
        lock.lock();

        try {
            operation = operations.remove();
        } finally {
            lock.unlock();
        }
        emptySpaces.release();
        return operation;
    }

    public int getSize() {
        lock.lock();
        try {
            return operations.size();
        } finally {
            lock.unlock();
        }
    }
}