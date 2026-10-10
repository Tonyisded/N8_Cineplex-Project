package com.nhom8.cineplex

import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.TokenVault
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class TokenVaultTest {
    @Test fun refreshTokenIsEncryptedAtRestAndClearedOnLogout() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vault = TokenVault(context)
        val original = vault.read()
        val token = "fixture-" + UUID.randomUUID()
        try {
            vault.write(token)
            assertEquals(token, TokenVault(context).read())
            val bytes = File(context.filesDir, "datastore/auth_session.preferences_pb").readBytes()
            assertFalse(String(bytes, Charsets.ISO_8859_1).contains(token))
            vault.write(null); assertNull(vault.read())
        } finally { vault.write(original) }
    }
}
