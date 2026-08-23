package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NoteTaker {
    private final Map<String, PendingJob> pendingJobs;
    private final ReentrantReadWriteLock lock;

    public NoteTaker() {
        pendingJobs = new HashMap<>();
        lock = new ReentrantReadWriteLock();
    }

    public void rememberJob(PendingJob job) {
        lock.writeLock().lock();
        try {
            String jobId = job.getOperation().getId();
            pendingJobs.put(jobId, job);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PendingJob removeJob(String jobId) {
        lock.writeLock().lock();

        try {
            return pendingJobs.remove(jobId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getSize() {
        lock.readLock().lock();
        try {
            return pendingJobs.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}