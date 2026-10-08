package com.cbgm.sparrow.runtime.startup

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.logging.StartupTrace
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

class ApplicationStartupRunner(
    private val startupTasks: List<StartupTask>,
    private val backgroundScope: CoroutineScope,
    private val foregroundRuntime: ForegroundRuntimeCoordinator
) : StartupRunner {
    private val logger = SparrowLog.withTag("ApplicationStartupRunner")
    private var postNavigationRuntimeStarted = false

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

    override fun startPostNavigationRuntime() {
        if (postNavigationRuntimeStarted) return
        postNavigationRuntimeStarted = true

        startBackgroundTasks()
        foregroundRuntime.start()
        StartupTrace.event("post-navigation runtime started")
    }

    private suspend fun runWaitingTasks(): List<StartupTaskResult> {
        val waitingTasks = startupTasks.filter(StartupTask::waitForCompletion)

        val completedWaitingTasks =
            coroutineScope {
                waitingTasks
                    .map { task ->
                        if (task.runOnMainThread) {
                            async { task to runTask(task) }
                        } else {
                            async(Dispatchers.Default) { task to runTask(task) }
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
