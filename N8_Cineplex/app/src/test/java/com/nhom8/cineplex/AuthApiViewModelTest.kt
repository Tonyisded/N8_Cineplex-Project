package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthApiViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private class FakeApi : AccountRepository {
        var user = Session("Customer", Role.CUSTOMER, "owned@example.test", "uuid", hasPassword = true, emailVerifiedAt = "2026-01-01T00:00:00Z")
        var restoreUser: Session? = null
        override suspend fun restore(): Session? = restoreUser
        var failure: ApiFailure? = null
        var loggedOut = false
        override fun validate(form: AuthForm, signup: Boolean) = validateAuth(form, signup)
        override suspend fun register(form: AuthForm) = RegistrationResult(form.email, true, "Check email")
        override suspend fun login(form: AuthForm, staff: Boolean): LoginResult { failure?.let { throw it }; return LoginResult.Success(user) }
        override suspend fun me(): Session { failure?.let { throw it }; return user }
        override suspend fun google(idToken: String) = user
        override suspend fun edit(name: String): Session { user = user.copy(name = name); return user }
        override suspend fun changePassword(current: String, replacement: String) = "Changed"
        override suspend fun logout() { loggedOut = true }
    }
    @Test fun networkFailureNeverCreatesMockSession() = runTest(dispatcher) {
        val api = FakeApi(); api.failure = ApiFailure("NETWORK_ERROR", "Offline")
        val vm = CineplexViewModel(api); advanceUntilIdle()
        vm.update(AuthForm(email = api.user.email, password = "secret-8")); vm.submit(); advanceUntilIdle()
        assertNull(vm.session); assertEquals(Screen.LOGIN, vm.screen); assertEquals("Offline", vm.feedback)
    }
    @Test fun staffRoutesToMinimalStaffHomeAndProfileUpdatesFromApi() = runTest(dispatcher) {
        val api = FakeApi(); api.user = api.user.copy(role = Role.STAFF)
        val vm = CineplexViewModel(api); advanceUntilIdle(); vm.navigate(Screen.STAFF)
        vm.update(AuthForm(email = api.user.email, password = "secret-8")); vm.submit(); advanceUntilIdle()
        assertEquals(Screen.STAFF_HOME, vm.screen)
        vm.account(); advanceUntilIdle(); vm.profileAction("edit"); assertEquals("edit", vm.dialog)
        vm.saveName("Edited"); advanceUntilIdle(); assertEquals("Edited", vm.session?.name)
        vm.back(); assertEquals(Screen.STAFF_HOME, vm.screen)
    }
    @Test fun googleOnlyPasswordIsNotEditableAndChangedEmailRequiresLogin() = runTest(dispatcher) {
        val api = FakeApi(); api.user = api.user.copy(hasPassword = false)
        val vm = CineplexViewModel(api); advanceUntilIdle(); vm.google(); assertTrue(vm.googleRequested)
        vm.googleSuccess("fixture-google-token"); advanceUntilIdle(); assertEquals(Screen.HOME, vm.screen)
        vm.account(); advanceUntilIdle(); vm.profileAction("password"); assertNull(vm.dialog); assertNotNull(vm.notice)
        vm.dismissNotice(); api.user = api.user.copy(email = "new@example.test"); vm.refreshProfile(); advanceUntilIdle()
        assertNull(vm.session); assertEquals(Screen.LOGIN, vm.screen); assertTrue(api.loggedOut)
    }
    @Test fun realValidationPreservesPlusAliasAndNeverTrimsPassword() {
        val api = FakeApi(); assertEquals("a+b@gmail.com", api.normalizeEmail(" A+B@GMAIL.COM "))
        val form = AuthForm("Name", "a+b@gmail.com", "        ", "        ", true)
        assertTrue(api.validate(form, true).isEmpty())
        assertTrue(api.validate(form.copy(password = "x".repeat(129)), true).containsKey("password"))
        assertFalse(form.toString().contains("password=        "))
    }

    @Test fun unverifiedSuccessNeverLoadsCatalog() = runTest(dispatcher) {
        val api = FakeApi(); api.user = api.user.copy(emailVerifiedAt = null)
        var calls = 0
        val catalog = object : CatalogRepository by MockCatalogRepository {
            override suspend fun movies(date: String): CatalogDay { calls++; return MockCatalogRepository.movies(date) }
            override suspend fun comingSoon(): List<Movie> { calls++; return emptyList() }
        }
        val vm = CineplexViewModel(api, catalog); advanceUntilIdle()
        vm.update(AuthForm(email = api.user.email, password = "secret-8")); vm.submit(); advanceUntilIdle()
        assertEquals(Screen.VERIFY_EMAIL, vm.screen); assertNull(vm.session); assertEquals(0, calls)
        vm.openMovie(MockMovies.all.first()); vm.customerHome(); vm.vouchers(); vm.tickets(); vm.refreshCatalog(); advanceUntilIdle()
        assertEquals(Screen.VERIFY_EMAIL, vm.screen); assertEquals(0, calls)
        vm.back(); assertEquals(Screen.LOGIN, vm.screen); assertEquals("", vm.form.password)
    }
    @Test fun backendVerificationFailureClearsCredentialsAndAllowsRetryAfterVerification() = runTest(dispatcher) {
        val api = FakeApi(); api.failure = ApiFailure("EMAIL_NOT_VERIFIED", "Verify", email = api.user.email)
        val vm = CineplexViewModel(api); advanceUntilIdle()
        vm.update(AuthForm(email=api.user.email,password="secret-8")); vm.submit(); advanceUntilIdle()
        assertEquals(Screen.VERIFY_EMAIL,vm.screen); assertEquals(api.user.email,vm.verificationEmail); assertEquals("",vm.form.password)
        vm.verificationLogin(); api.failure=null; vm.update(vm.form.copy(password="secret-8")); vm.submit(); advanceUntilIdle()
        assertEquals(Screen.HOME,vm.screen)
    }
    @Test fun restoredUnverifiedSessionAlwaysReturnsToVerification() = runTest(dispatcher) {
        val api = FakeApi(); api.restoreUser = api.user.copy(emailVerifiedAt = null)
        val vm = CineplexViewModel(api); advanceUntilIdle()
        assertEquals(Screen.VERIFY_EMAIL,vm.screen);assertNull(vm.session);assertEquals(api.user.email,vm.verificationEmail)
    }
}
