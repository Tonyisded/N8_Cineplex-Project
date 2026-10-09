package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CineplexViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun vm() = CineplexViewModel(MockAccountRepository())
    private fun user() = AuthForm(email = "user@cineplex.test",password = "123456")
    @Test fun initialScreenAndAuthBackClearCredentials() {
        val vm = vm(); assertEquals(Screen.LOGIN,vm.screen)
        listOf(Screen.STAFF,Screen.SIGNUP).forEach { vm.navigate(it); vm.update(user()); vm.back(); assertEquals(Screen.LOGIN,vm.screen); assertEquals(AuthForm(),vm.form) }
    }
    @Test fun loadingBlocksDuplicateSubmitAndFieldChanges() = runTest(dispatcher) {
        val vm = vm(); vm.update(user()); vm.submit(); vm.submit()
        assertTrue(vm.loading); vm.update(AuthForm()); assertEquals(user(),vm.form)
        advanceUntilIdle(); assertFalse(vm.loading); assertEquals(Screen.HOME,vm.screen); assertEquals("",vm.form.password)
    }
    @Test fun roleErrorsAndAdminDashboard() = runTest(dispatcher) {
        val vm = vm(); vm.navigate(Screen.STAFF); vm.update(user()); vm.submit(); advanceUntilIdle()
        assertNull(vm.session); assertEquals("Tài khoản không có quyền truy cập",vm.feedback)
        vm.navigate(Screen.LOGIN); vm.update(user().copy(email = "admin@cineplex.test")); vm.submit(); advanceUntilIdle()
        assertNull(vm.session); assertTrue(vm.staffRequired)
        vm.navigate(Screen.STAFF); vm.update(user().copy(email = "admin@cineplex.test")); vm.submit(); advanceUntilIdle()
        assertEquals(Screen.ADMIN,vm.screen); vm.logout(); vm.back(); assertEquals(Screen.LOGIN,vm.screen); assertNull(vm.session)
    }
    @Test fun registerPrefillLoginLogoutAndRetention() = runTest(dispatcher) {
        val vm = vm(); vm.navigate(Screen.SIGNUP); vm.update(AuthForm("An"," NEW@test.com ","abcdefgh","abcdefgh",true)); vm.submit(); advanceUntilIdle()
        assertEquals(Screen.LOGIN,vm.screen); assertEquals(AuthForm(email = "new@test.com"),vm.form); assertTrue(vm.success)
        vm.update(vm.form.copy(password = "abcdefgh")); vm.submit(); advanceUntilIdle(); assertEquals(Role.USER,vm.session?.role)
        vm.query = "Dune"; vm.soon = true; vm.account(); assertEquals(Screen.PROFILE,vm.screen); assertNull(vm.notice); assertEquals("new@test.com",vm.session?.email); vm.logout(); vm.back()
        assertNull(vm.session); assertNull(vm.notice); assertEquals("",vm.query); assertFalse(vm.soon); assertEquals(Screen.LOGIN,vm.screen)
        vm.update(AuthForm(email = "new@test.com",password = "abcdefgh")); vm.submit(); advanceUntilIdle(); assertEquals(Screen.HOME,vm.screen)
    }
    @Test fun leavingPendingRegistrationCancelsCreation() = runTest(dispatcher) {
        val vm = vm(); vm.navigate(Screen.SIGNUP)
        val form = AuthForm("An","cancel@test.com","abcdefgh","abcdefgh",true)
        vm.update(form); vm.submit(); runCurrent(); advanceTimeBy(300); vm.back(); advanceUntilIdle()
        assertFalse(vm.loading); vm.update(form); vm.submit(); advanceUntilIdle(); assertNull(vm.session); assertTrue(vm.feedback.startsWith("Sai tài khoản"))
    }
    @Test fun leavingPendingLoginCancelsSession() = runTest(dispatcher) {
        val vm = vm(); vm.update(user()); vm.submit(); runCurrent(); vm.navigate(Screen.STAFF); advanceUntilIdle()
        assertNull(vm.session); assertFalse(vm.loading); assertEquals(Screen.STAFF,vm.screen)
    }
    @Test fun detailsBackPreservesFiltersAndLogoutGuardsDetails() = runTest(dispatcher) {
        val vm = vm(); vm.update(user()); vm.submit(); advanceUntilIdle(); vm.query = "linh hon"; vm.soon = true
        vm.openMovie(MockMovies.all.last()); vm.back(); assertEquals(Screen.HOME,vm.screen); assertEquals("linh hon",vm.query); assertTrue(vm.soon)
        vm.logout(); vm.openMovie(MockMovies.all.first()); vm.back(); assertNull(vm.movie); assertEquals(Screen.LOGIN,vm.screen)
    }
    @Test fun dialogBackKeepsScreenAndForm() {
        val vm = vm(); vm.navigate(Screen.SIGNUP); vm.update(user()); vm.terms(false); vm.back()
        assertNull(vm.notice); assertEquals(Screen.SIGNUP,vm.screen); assertEquals(user(),vm.form)
    }
    @Test fun profileUsesNormalizedSessionAndReturnsToRoleHome() = runTest(dispatcher) {
        val vm = vm()
        vm.account(); assertEquals(Screen.LOGIN, vm.screen)
        vm.update(user().copy(email = " USER@CINEPLEX.TEST ")); vm.submit(); advanceUntilIdle()
        vm.query = "Dune"; vm.soon = true; vm.account()
        assertEquals(Screen.PROFILE, vm.screen)
        assertEquals(Session("Khách hàng", Role.USER, "user@cineplex.test"), vm.session)
        vm.profileAction("edit"); vm.back()
        assertEquals(Screen.PROFILE, vm.screen); assertNull(vm.notice)
        vm.back(); assertEquals(Screen.HOME, vm.screen); assertEquals("Dune", vm.query); assertTrue(vm.soon)
        vm.logout(); vm.account(); vm.profileAction("help")
        assertEquals(Screen.LOGIN, vm.screen); assertNull(vm.session); assertNull(vm.notice)
        vm.navigate(Screen.STAFF); vm.update(user().copy(email = "admin@cineplex.test")); vm.submit(); advanceUntilIdle()
        vm.account(); assertEquals(Screen.PROFILE, vm.screen); assertEquals("admin@cineplex.test", vm.session?.email)
        vm.back(); assertEquals(Screen.ADMIN, vm.screen)
    }
    @Test fun googlePlaceholderNeverCreatesSessionAndIsBlockedWhileLoading() = runTest(dispatcher) {
        val vm = vm(); vm.google()
        assertEquals("Chức năng sẽ được bổ sung", vm.notice?.title); assertNull(vm.session)
        vm.back(); assertEquals(Screen.LOGIN, vm.screen)
        listOf(Screen.SIGNUP, Screen.STAFF).forEach {
            vm.navigate(it); vm.google(); assertNull(vm.notice)
        }
        vm.navigate(Screen.LOGIN); vm.update(user()); vm.submit(); vm.google()
        assertNull(vm.notice); advanceUntilIdle()
        assertEquals(Screen.HOME, vm.screen); vm.google(); assertNull(vm.notice)
    }
    @Test fun sixMoviesSearchWithoutAccentsAndTabFiltering() {
        assertEquals(6,MockMovies.all.size); assertEquals(4,MockMovies.search("",false).size); assertEquals(2,MockMovies.search("",true).size)
        assertEquals("spirited",MockMovies.search("VUNG DAT LINH HON",true).single().id)
        assertEquals("dune",MockMovies.search(" DUNE: PART TWO ",false).single().id)
        assertTrue(MockMovies.search("no such movie",false).isEmpty())
    }
}
