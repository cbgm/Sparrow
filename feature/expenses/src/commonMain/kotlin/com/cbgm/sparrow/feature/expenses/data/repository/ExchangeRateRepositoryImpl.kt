package com.cbgm.sparrow.feature.expenses.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.expenses.data.datasource.LocalExchangeRateDataSource
import com.cbgm.sparrow.feature.expenses.data.datasource.RemoteExchangeRateDataSource
import com.cbgm.sparrow.feature.expenses.data.mapper.toExchangeRates
import com.cbgm.sparrow.feature.expenses.data.mapper.toExchangeRatesCacheDto
import com.cbgm.sparrow.feature.expenses.domain.model.ExchangeRates
import com.cbgm.sparrow.feature.expenses.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ExchangeRateRepositoryImpl(
    private val localDataSource: LocalExchangeRateDataSource,
    private val remoteDataSource: RemoteExchangeRateDataSource
) : ExchangeRateRepository {
    private val refreshMutex = Mutex()

    override fun observeRates(): Flow<ExchangeRates?> =
        localDataSource.observe().map { cached -> cached?.toExchangeRates() }

    override suspend fun refreshIfStale(): Result<Unit> =
        safeSuspendCall {
            refreshMutex.withLock {
                val now = SystemClock.nowEpochMilliseconds()
                val cachedAt = localDataSource.get()?.fetchedAtEpochMilliseconds
                if (cachedAt != null && now - cachedAt < MAX_AGE_MILLISECONDS) return@withLock
                localDataSource.save(remoteDataSource.fetchEuroRates().toExchangeRatesCacheDto(now))
            }
        }

    private companion object {
        const val MAX_AGE_MILLISECONDS = 24 * 60 * 60 * 1000L
    }
}
