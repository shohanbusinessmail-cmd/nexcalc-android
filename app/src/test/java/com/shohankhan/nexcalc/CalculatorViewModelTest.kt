package com.shohankhan.nexcalc

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.CalculatorViewModel
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.Complex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalculatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val container: AppContainer = AppContainer(ApplicationProvider.getApplicationContext<Context>())

    private fun viewModel() = CalculatorViewModel(container, HistorySource.STANDARD)

    @Test
    fun `respects operator precedence`() {
        val vm = viewModel()
        vm.evaluate("2+3*4")
        assertEquals("14", vm.state.value.outcome?.value)
    }

    @Test
    fun `parentheses override precedence`() {
        val vm = viewModel()
        vm.evaluate("(2+3)*4")
        assertEquals("20", vm.state.value.outcome?.value)
    }

    @Test
    fun `division produces the expected decimal`() {
        val vm = viewModel()
        vm.evaluate("10/2")
        assertEquals("5", vm.state.value.outcome?.value)
    }

    @Test
    fun `division by zero surfaces a structured error`() {
        val vm = viewModel()
        vm.evaluate("10/0")
        assertNotNull(vm.state.value.outcome?.error)
        assertTrue(vm.state.value.outcome?.error?.message?.isNotBlank() == true)
    }

    @Test
    fun `ans refers to the previous exact result`() {
        val vm = viewModel()
        vm.evaluate("6*7")
        assertEquals(Complex(42.0), vm.state.value.ans)
        vm.evaluate("ans+1")
        assertEquals("43", vm.state.value.outcome?.value)
    }

    @Test
    fun `degree mode is the default for trigonometry`() {
        val vm = viewModel()
        vm.evaluate("sin(30)")
        assertEquals("0.5", vm.state.value.outcome?.value)
    }

    @Test
    fun `memory store and clear work`() {
        val vm = viewModel()
        vm.storeMemory(Complex(12.5))
        assertEquals(12.5, vm.state.value.memory, 0.0)
        assertTrue(vm.state.value.memorySet)
        assertEquals("12.5", vm.memoryText())
        vm.clearMemory()
        assertTrue(!vm.state.value.memorySet)
    }

    @Test
    fun `memory accumulates results`() {
        val vm = viewModel()
        vm.addToMemory(Complex(3.0))
        vm.addToMemory(Complex(4.0))
        assertEquals(7.0, vm.state.value.memory, 0.0)
        vm.subtractFromMemory(Complex(2.0))
        assertEquals(5.0, vm.state.value.memory, 0.0)
    }

    @Test
    fun `blank expression shows a validation error`() {
        val vm = viewModel()
        vm.evaluate("   ")
        assertNotNull(vm.state.value.outcome?.error)
    }
}
