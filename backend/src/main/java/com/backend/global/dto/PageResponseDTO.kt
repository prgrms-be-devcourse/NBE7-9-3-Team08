package com.backend.global.dto

import org.springframework.data.domain.Page

data class PageResponseDTO<T>(
    val content: MutableList<T>, // 실제 데이터 목록
    val page: Int, // 현재 페이지 번호
    val size: Int, // 페이지당 데이터 수
    val totalPages: Int, // 전체 페이지 수
    val totalElements: Long, // 전체 데이터 수
    val last: Boolean // 마지막 페이지 여부
) {
    constructor(pageData: Page<T>): this (
        content = pageData.content,
        page = pageData.number,
        size = pageData.size,
        totalPages = pageData.totalPages,
        totalElements = pageData.totalElements,
        last = pageData.isLast
    )
}