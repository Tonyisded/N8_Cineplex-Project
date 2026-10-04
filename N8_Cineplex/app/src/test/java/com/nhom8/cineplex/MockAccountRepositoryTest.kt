package com.nhom8.cineplex

import com.nhom8.cineplex.data.MockAccountRepository
import com.nhom8.cineplex.model.*
import org.junit.Test
import org.junit.Assert.*

class MockAccountRepositoryTest {
    private fun form(email: String = "new@test.com") = AuthForm(" Nguyen An ",email,"abcdefgh","abcdefgh",true)
    @Test fun seedLoginAndAllRoleBoundaries() {
        val repo = MockAccountRepository()
        val user = AuthForm(email = " USER@cineplex.test ",password = "123456")
        val admin = user.copy(email = "admin@cineplex.test")
        assertTrue(repo.validate(user,false).isEmpty())
        assertEquals(Role.USER,(repo.login(user,false) as LoginResult.Success).session.role)
        assertEquals(Role.ADMIN,(repo.login(admin,true) as LoginResult.Success).session.role)
        assertEquals(LoginResult.Forbidden,repo.login(user,true))
        assertEquals(LoginResult.StaffRequired,repo.login(admin,false))
        assertEquals(LoginResult.InvalidCredentials,repo.login(user.copy(password = "wrong"),false))
        assertEquals(LoginResult.InvalidCredentials,repo.login(user.copy(email = "unknown@test.com"),false))
    }
    @Test fun allRequiredFieldsAndInvalidEmail() {
        val repo = MockAccountRepository()
        assertEquals(setOf("name","email","password","confirm","consent"),repo.register(AuthForm()).keys)
        assertEquals(setOf("name","email","password","confirm","consent"),repo.register(form().copy(name = " ",email = "wrong",password = "1234567",confirm = "different",consent = false)).keys)
        assertEquals(setOf("email"),repo.validate(form().copy(email = "name @test.com"),true).keys)
        assertEquals(setOf("email","password"),repo.validate(AuthForm(),false).keys)
    }
    @Test fun normalizedDuplicateEmailsAndAlwaysUser() {
        val repo = MockAccountRepository()
        val form = form(" NEW@TEST.COM ")
        assertTrue(repo.register(form).isEmpty())
        assertEquals(Session("Nguyen An",Role.USER),(repo.login(form.copy(email = "new@test.com"),false) as LoginResult.Success).session)
        assertEquals(LoginResult.Forbidden,repo.login(form,true))
        assertEquals(setOf("email"),repo.register(form("new@test.com")).keys)
        listOf(" USER@cineplex.test ","ADMIN@CINEPLEX.TEST").forEach { assertEquals(setOf("email"),repo.register(form(it)).keys) }
    }
    @Test fun passwordBoundaryConfirmationAndConsent() {
        val repo = MockAccountRepository()
        assertEquals(setOf("password"),repo.validate(form().copy(password = "1234567",confirm = "1234567"),true).keys)
        assertTrue(repo.validate(form().copy(password = "12345678",confirm = "12345678"),true).isEmpty())
        assertEquals(setOf("confirm"),repo.validate(form().copy(confirm = "bad"),true).keys)
        assertEquals(setOf("confirm"),repo.validate(form().copy(confirm = ""),true).keys)
        assertEquals(setOf("consent"),repo.validate(form().copy(consent = false),true).keys)
    }
    @Test fun newRepositoryDoesNotPersistAccounts() {
        val repo = MockAccountRepository(); repo.register(form())
        assertEquals(LoginResult.InvalidCredentials,MockAccountRepository().login(form(),false))
    }
    @Test fun formDiagnosticRedactsPasswords() {
        val form = form()
        assertFalse(form.toString().contains(form.password))
        assertTrue(form.toString().contains("<redacted>"))
    }
}
