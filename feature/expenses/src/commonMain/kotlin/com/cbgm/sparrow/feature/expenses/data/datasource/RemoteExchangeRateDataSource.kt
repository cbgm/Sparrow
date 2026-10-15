package com.cbgm.sparrow.feature.expenses.data.datasource

import com.cbgm.sparrow.feature.expenses.data.model.ExchangeRateDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class RemoteExchangeRateDataSource(
    private val httpClient: HttpClient
) {
    suspend fun fetchEuroRates(): List<ExchangeRateDto> {
        val response = httpClient.get(EURO_RATES_URL)
        check(response.status.isSuccess()) {
            "Exchange rates could not be loaded: ${response.status}"
        }
        return json.decodeFromString(response.bodyAsText())
    }

    private companion object {
        const val EURO_RATES_URL = "https://api.frankfurter.dev/v2/rates?base=EUR"
        val json = Json { ignoreUnknownKeys = true }
    }
}
