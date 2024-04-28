package com.thiagotoazza.data.models.report

data class ReportRequest(
    val date: Long,
    val services: List<String>
)
