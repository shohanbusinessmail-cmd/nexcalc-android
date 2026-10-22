package com.shohankhan.nexcalc

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.formulas.FormulaDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormulaDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val container: AppContainer = AppContainer(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `heron triangle with 3 4 5 has area 6`() {
        val vm = FormulaDetailViewModel(container, "triangle-heron")
        vm.setInput("a", "3")
        vm.setInput("b", "4")
        vm.setInput("c", "5")
        vm.calculate()
        val primary = vm.state.value.result?.values?.firstOrNull { it.primary }
        assertNotNull(primary)
        assertEquals("6", primary?.value)
        assertNull(vm.state.value.failure)
    }

    @Test
    fun `defaults are prefilled from the catalogue`() {
        val vm = FormulaDetailViewModel(container, "square-area")
        assertEquals("5", vm.state.value.inputs.first { it.id == "side" }.value)
    }

    @Test
    fun `invalid input is reported per field`() {
        val vm = FormulaDetailViewModel(container, "triangle-heron")
        vm.setInput("a", "-1")
        vm.calculate()
        assertNotNull(vm.state.value.inputs.first { it.id == "a" }.error)
        assertTrue(vm.state.value.result == null)
    }

    @Test
    fun `impossible triangle is a domain failure`() {
        val vm = FormulaDetailViewModel(container, "triangle-heron")
        vm.setInput("a", "1")
        vm.setInput("b", "1")
        vm.setInput("c", "10")
        vm.calculate()
        assertNotNull(vm.state.value.failure)
    }

    @Test
    fun `reset restores defaults`() {
        val vm = FormulaDetailViewModel(container, "square-area")
        vm.setInput("side", "12")
        vm.reset()
        assertEquals("5", vm.state.value.inputs.first().value)
    }
}
