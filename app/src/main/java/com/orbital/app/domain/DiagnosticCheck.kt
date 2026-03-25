package com.orbital.app.domain

data class DiagnosticCheck(
    val label: String,
    val ok: Boolean,
    val details: String = ""
)
