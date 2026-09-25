package com.cbgm.sparrow.feature.linkpreview.data.model

class LinkPreviewUnavailableException(
    url: String
) : Exception("No link preview image available for $url")
