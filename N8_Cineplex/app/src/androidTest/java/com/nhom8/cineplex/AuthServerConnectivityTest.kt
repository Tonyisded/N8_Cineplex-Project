package com.nhom8.cineplex

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class AuthServerConnectivityTest {
    @Test fun deviceReachesVpsAuthOverVerifiedHttps() {
        assertEquals("https://18.143.100.43/api/v1/", BuildConfig.API_BASE_URL)
        val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
        client.newCall(Request.Builder().url(BuildConfig.API_BASE_URL + "health").build()).execute().use {
            assertEquals(200, it.code)
            assertTrue(requireNotNull(it.body).string().contains("\"database\":\"up\""))
            assertNotNull(it.handshake)
        }
        val request = Request.Builder().url(BuildConfig.API_BASE_URL + "auth/login")
            .post("{}".toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { assertEquals(400, it.code) }
    }
}
