package com.prowllabs.prowl.core

import com.prowllabs.prowl.core.model.NetworkLog

internal fun makeLog(
    statusCode: Int? = 200,
    startedAtMillis: Long = System.currentTimeMillis(),
    url: String = "https://api.example.com/users",
    method: String = "GET",
): NetworkLog = NetworkLog(
    url = url,
    method = method,
    statusCode = statusCode,
    startedAtMillis = startedAtMillis,
    durationMillis = 120,
)
