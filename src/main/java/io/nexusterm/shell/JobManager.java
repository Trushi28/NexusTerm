package io.nexusterm.shell;

import java.util.Comparator;
import java.util.LinkedHashMap;
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
        return job;
    }

    public java.util.Optional<Job> getJob(int id) {
        return java.util.Optional.ofNullable(jobs.get(id));
    }
    
    public Map<Integer, Job> listJobs() {
        return jobs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(LinkedHashMap::new, (acc, entry) -> acc.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    public int runningCount() {
        int running = 0;
        for (Job job : jobs.values()) {
            if (!job.future().isDone()) {
                running++;
            }
        }
        return running;
    }
}
