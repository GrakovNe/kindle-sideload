package org.grakovne.sideload.kindle

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * A no-op scheduler: the `@EnableScheduling` post-processor schedules every `@Scheduled` method
 * onto the `taskScheduler` bean, so pointing it at a scheduler that never fires keeps the
 * periodic workers (the 100 ms converter, the 5 s STK and TTL workers, the converter binary
 * update task) from running in the background and corrupting the shared test state.
 *
 * Shared by [TestDatabase] and the acceptance scenarios; tests that need a periodic worker to
 * run drive it manually instead.
 */
@Configuration
open class NoOpScheduling {
    // a single parked task keeps the executor alive until Spring shuts it down at context close
    @Bean(destroyMethod = "shutdown")
    open fun neverFiringExecutor(): ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)

    @Bean
    open fun taskScheduler(executor: ScheduledThreadPoolExecutor): TaskScheduler {
        val never: ScheduledFuture<*> = executor.schedule(Runnable {}, Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        return object : TaskScheduler {
            override fun schedule(task: Runnable, trigger: Trigger): ScheduledFuture<*> = never
            override fun schedule(task: Runnable, startTime: Instant): ScheduledFuture<*> = never
            override fun scheduleAtFixedRate(task: Runnable, startTime: Instant, period: Duration): ScheduledFuture<*> = never
            override fun scheduleAtFixedRate(task: Runnable, period: Duration): ScheduledFuture<*> = never
            override fun scheduleWithFixedDelay(task: Runnable, startTime: Instant, delay: Duration): ScheduledFuture<*> = never
            override fun scheduleWithFixedDelay(task: Runnable, delay: Duration): ScheduledFuture<*> = never
        }
    }
}
