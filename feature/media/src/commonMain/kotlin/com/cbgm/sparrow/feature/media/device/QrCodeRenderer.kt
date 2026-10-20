package com.cbgm.sparrow.feature.media.device

/** UTF-8 QR image, optionally followed by readable text, encoded as an opaque PNG. */
expect fun renderQrCode(request: QrCodeRenderRequest): ByteArray
