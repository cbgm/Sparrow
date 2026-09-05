package com.cbgm.sparrow.feature.settings.data.datasource

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowErrorSink
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.settings.data.model.DeveloperErrorDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class DeveloperErrorLogStorageDataSource(
    private val dataStore: SparrowDataStore
) : SparrowErrorSink {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private val serializer = ListSerializer(DeveloperErrorDto.serializer())
    private val commands = Channel<Command>(capacity = Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            for (command in commands) {
                when (command) {
                    is Command.Record -> {
                        try {
                            persist(command.error)
                        } catch (_: Throwable) {
                            // Error logging must never terminate the storage actor.
                        }
                    }

                    is Command.Clear -> {
                        try {
                            clearPersistedErrors()
                            command.completion.complete(Unit)
                        } catch (error: Throwable) {
                            command.completion.completeExceptionally(error)
                        }
                    }
                }
            }
        }
    }

    fun observeErrors(): Flow<List<DeveloperErrorDto>> =
        dataStore
            .observeString(KEY_ERRORS)
            .map(::decodeErrors)
            .distinctUntilChanged()

    suspend fun clearErrors() {
        val completion = CompletableDeferred<Unit>()
        commands.send(Command.Clear(completion))
        completion.await()
    }

    override fun record(
        tag: String,
        timestampEpochMilliseconds: Long,
        message: String,
        throwable: Throwable?
    ) {
        commands.trySend(
            Command.Record(
                DeveloperErrorDto(
                    id = IdGenerator.generate(prefix = ERROR_ID_PREFIX),
                    timestampEpochMilliseconds = timestampEpochMilliseconds,
                    tag = tag.take(MAX_TAG_LENGTH),
                    message = message.take(MAX_MESSAGE_LENGTH),
                    exceptionType = throwable?.let { it::class.simpleName ?: FALLBACK_EXCEPTION_TYPE },
                    stackTrace = throwable?.stackTraceToString()?.take(MAX_STACK_TRACE_LENGTH)
                )
            )
        )
    }

    private suspend fun persist(error: DeveloperErrorDto) {
        val currentErrors = decodeErrors(dataStore.getString(KEY_ERRORS))
        val updatedErrors =
            buildList {
                add(error)
                addAll(currentErrors)
            }.take(MAX_SAVED_ERRORS)

        dataStore.edit {
            putString(
                key = KEY_ERRORS,
                value = json.encodeToString(serializer, updatedErrors)
            )
        }
    }

    private suspend fun clearPersistedErrors() {
        dataStore.edit {
            removeString(KEY_ERRORS)
        }
    }

    private fun decodeErrors(value: String?): List<DeveloperErrorDto> {
        if (value.isNullOrBlank()) return emptyList()

        return try {
            json.decodeFromString(serializer, value)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private sealed interface Command {
        data class Record(
            val error: DeveloperErrorDto
        ) : Command

        data class Clear(
            val completion: CompletableDeferred<Unit>
        ) : Command
    }

    private companion object {
        const val KEY_ERRORS = "settings.developer_error_log.errors"
        const val ERROR_ID_PREFIX = "developer-error"
        const val FALLBACK_EXCEPTION_TYPE = "Throwable"
        const val MAX_SAVED_ERRORS = 250
        const val MAX_TAG_LENGTH = 128
        const val MAX_MESSAGE_LENGTH = 4_096
        const val MAX_STACK_TRACE_LENGTH = 16_384
    }
}
