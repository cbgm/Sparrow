package com.cbgm.sparrow.feature.attachments.di

import com.cbgm.sparrow.core.crypto.blob.BlobCipher
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.feature.attachments.data.BlobTransferRepositoryImpl
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.DownloadBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.UploadBlobUseCase
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

val attachmentsModule =
    module {
        single<BlobTransferRepository> {
            BlobTransferRepositoryImpl(
                httpClient = get<HttpClient>(),
                webSocketTransportClient = get<WebSocketTransportClient>(),
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                localRoutingIdProvider = get<LocalRoutingIdProvider>(),
                blobCipher = get<BlobCipher>(),
                cryptoHash = get<CryptoHash>(),
                secureRandomGenerator = get<SecureRandomGenerator>()
            )
        }

        factory {
            UploadBlobUseCase(repository = get<BlobTransferRepository>())
        }

        factory {
            DownloadBlobUseCase(repository = get<BlobTransferRepository>())
        }

        factory {
            DeleteBlobUseCase(repository = get<BlobTransferRepository>())
        }
    }
