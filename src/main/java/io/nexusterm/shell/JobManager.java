package io.nexusterm.shell;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages background and foreground jobs in NexusTerm.
 */
public class JobManager {
    private final Map<Integer, Job> jobs = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public Job createJob(String command, java.util.concurrent.CompletableFuture<?> future) {
        int id = nextId.getAndIncrement();
        Job job = new Job(id, command, future);
        jobs.put(id, job);
        
        future.handle((res, ex) -> {
            // Clean up or notify on completion
            return null;
        });
        
        return job;
    }

    public java.util.Optional<Job> getJob(int id) {
        return java.util.Optional.ofNullable(jobs.get(id));
    }
    
    public Map<Integer, Job> listJobs() {
        return jobs;
    }
}
