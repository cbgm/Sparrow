package com.cbgm.sparrow.feature.expenses.data.datasource

import com.cbgm.sparrow.feature.expenses.data.model.ExchangeRateDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** The connected node fetches the rates, so the rate provider never sees the user's address. */
class RemoteExchangeRateDataSource(
    private val httpClient: HttpClient
) {
    suspend fun fetchEuroRates(nodeBaseUrl: String): List<ExchangeRateDto> {
        val response = httpClient.get("$nodeBaseUrl/v1/exchange-rates")
        check(response.status.isSuccess()) {
            "Exchange rates could not be loaded: ${response.status}"
        }
        return json.decodeFromString(response.bodyAsText())
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
