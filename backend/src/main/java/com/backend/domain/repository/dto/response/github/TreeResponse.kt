package com.backend.domain.repository.dto.response.github

data class TreeResponse(
    val tree: List<TreeItem>,
    val truncated: Boolean
) {
    data class TreeItem(
        val path: String?,
        val type: String?
    )
}
