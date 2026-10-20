package com.cbgm.sparrow.runtime.startup

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.logging.StartupTrace
import com.cbgm.sparrow.runtime.foreground.ForegroundRuntimeCoordinator
import com.cbgm.sparrow.startup.domain.model.StartupResult
import com.cbgm.sparrow.startup.domain.runner.StartupRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs the startup tasks in two groups.
 *
 * Waiting tasks ([StartupTask.waitForCompletion]) run in parallel and decide the startup result
 * that releases the splash screen. All other tasks run in the background once the runtime starts.
 *
 * The runtime (background tasks plus the foreground transport) does not wait for navigation:
 * it starts as soon as every waiting task marked [StartupTask.requiredForTransport] has finished
 * and the local identity is ready. Slower waiting tasks such as the app language keep running
 * meanwhile, so the connection is not delayed by work it does not depend on. When no identity
 * exists yet, the runtime starts later through [startPostNavigationRuntime].
 */
class ApplicationStartupRunner(
    private val startupTasks: List<StartupTask>,
    private val backgroundScope: CoroutineScope,
    private val foregroundRuntime: ForegroundRuntimeCoordinator
) : StartupRunner {
    private val logger = SparrowLog.withTag("ApplicationStartupRunner")

    // Locked on first start and never released, so the runtime starts exactly once.
    private val runtimeStart = Mutex()
    private val transportPrerequisites = Mutex()
    private var finishedTransportPrerequisites = 0
    private var identityReady = false

    override suspend fun run(): StartupResult =
        try {
            val waitingTaskResults = runWaitingTasks()
            resolveStartupResult(waitingTaskResults)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            logger.error(error) { "Required application initialization failed" }
            StartupResult.Error(error)
        }

    // Fallback for starts without a ready identity; a no-op if the runtime already started.
    override fun startPostNavigationRuntime() {
        startRuntime()
    }

    private fun startRuntime() {
        if (!runtimeStart.tryLock()) return

        startBackgroundTasks()
        foregroundRuntime.start()
        StartupTrace.event("runtime started")
    }

    // Starts the runtime early once all transport prerequisites have finished and one of them
    // reported a ready identity. Tasks finish on different threads, hence the lock.
    private suspend fun onWaitingTaskFinished(
        task: StartupTask,
        result: StartupTaskResult,
        prerequisiteCount: Int
    ) {
        if (!task.requiredForTransport) return
        val prerequisitesReady =
            transportPrerequisites.withLock {
                finishedTransportPrerequisites++
                if (result == StartupTaskResult.IdentityReady) identityReady = true
                identityReady && finishedTransportPrerequisites == prerequisiteCount
            }
        if (prerequisitesReady) startRuntime()
    }

    private suspend fun runWaitingTasks(): List<StartupTaskResult> {
        val waitingTasks = startupTasks.filter(StartupTask::waitForCompletion)
        val prerequisiteCount = waitingTasks.count(StartupTask::requiredForTransport)

        val completedWaitingTasks =
            coroutineScope {
                waitingTasks
                    .map { task ->
                        if (task.runOnMainThread) {
                            async { task to runWaitingTask(task, prerequisiteCount) }
                        } else {
                            async(Dispatchers.Default) { task to runWaitingTask(task, prerequisiteCount) }
                        }
                    }.awaitAll()
            }

        val unfinishedWaitingTasks =
            waitingTasks.filterNot { task ->
                completedWaitingTasks.any { (completedTask, _) -> completedTask === task }
            }

        check(unfinishedWaitingTasks.isEmpty()) {
            "Startup waiting tasks did not finish: ${
                unfinishedWaitingTasks.joinToString { task -> task.name }
            }"
        }

        StartupTrace.event("all startup waiting tasks finished")

        return completedWaitingTasks.map { (_, result) -> result }
    }

    private fun resolveStartupResult(results: List<StartupTaskResult>): StartupResult {
        val identityResults =
            results.filter { result ->
                result == StartupTaskResult.IdentityReady ||
                    result == StartupTaskResult.IdentityRequired
            }

        check(identityResults.size == 1) {
            "Expected exactly one startup identity result, but found ${identityResults.size}"
        }

        return when (identityResults.single()) {
            StartupTaskResult.IdentityReady -> StartupResult.Ready
            StartupTaskResult.IdentityRequired -> StartupResult.IdentityRequired
            StartupTaskResult.Completed -> error("Unexpected completed identity result")
        }
    }

    private fun startBackgroundTasks() {
        startupTasks
            .filterNot(StartupTask::waitForCompletion)
            .forEach { task ->
                backgroundScope.launch(Dispatchers.Default) {
                    runBackgroundTask(task)
                }
            }
    }

    private suspend fun runWaitingTask(
        task: StartupTask,
        prerequisiteCount: Int
    ): StartupTaskResult =
        runTask(task).also { result -> onWaitingTaskFinished(task, result, prerequisiteCount) }

    private suspend fun runTask(task: StartupTask): StartupTaskResult =
        StartupTrace.measure(task.name) {
            task.run()
        }

    private suspend fun runBackgroundTask(task: StartupTask) {
        try {
            StartupTrace.measure(task.name) {
                task.run()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            logger.error(error) {
                "Background startup task failed: ${task.name}"
            }
        }
    }
}
