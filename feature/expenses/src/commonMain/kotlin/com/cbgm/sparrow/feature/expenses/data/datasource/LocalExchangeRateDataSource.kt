package com.cbgm.sparrow.feature.expenses.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.expenses.data.model.ExchangeRatesCacheDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class LocalExchangeRateDataSource(
    private val dataStore: SparrowDataStore
) {
    fun observe(): Flow<ExchangeRatesCacheDto?> =
        dataStore.observeString(KEY_EXCHANGE_RATES).map { value -> value?.decode() }

    suspend fun get(): ExchangeRatesCacheDto? = dataStore.getString(KEY_EXCHANGE_RATES)?.decode()

    suspend fun save(rates: ExchangeRatesCacheDto) {
        val value = Json.encodeToString(ExchangeRatesCacheDto.serializer(), rates)
        dataStore.edit { putString(KEY_EXCHANGE_RATES, value) }
    }

    private fun String.decode(): ExchangeRatesCacheDto? =
        runCatching { Json.decodeFromString(ExchangeRatesCacheDto.serializer(), this) }.getOrNull()

    private companion object {
        const val KEY_EXCHANGE_RATES = "expenses.exchange_rates"
    }
}
