package com.shohankhan.nexcalc

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.converters.ConverterViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConverterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val container: AppContainer = AppContainer(ApplicationProvider.getApplicationContext<Context>())

    private fun viewModel() = ConverterViewModel(container)

    @Test
    fun `metres to feet uses the exact international foot`() {
        val vm = viewModel()
        vm.selectCategory("length")
        vm.selectFrom("m")
        vm.selectTo("ft")
        vm.setInput("1")
        assertEquals("3.280839895", vm.state.value.result)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `fahrenheit conversion uses the offset scale`() {
        val vm = viewModel()
        vm.selectCategory("temperature")
        vm.selectFrom("c")
        vm.selectTo("f")
        vm.setInput("100")
        assertEquals("212", vm.state.value.result)
    }

    @Test
    fun `swapping units reverses the conversion`() {
        val vm = viewModel()
        vm.selectCategory("length")
        vm.selectFrom("m")
        vm.selectTo("ft")
        vm.setInput("1")
        vm.swap()
        assertEquals("0.3048", vm.state.value.result)
    }

    @Test
    fun `invalid input reports an error instead of a value`() {
        val vm = viewModel()
        vm.setInput("abc")
        assertNotNull(vm.state.value.error)
        assertEquals("", vm.state.value.result)
    }

    @Test
    fun `changing category resets the unit pair to that dimension`() {
        val vm = viewModel()
        vm.selectCategory("mass")
        assertEquals("mass", vm.state.value.categoryId)
        assertEquals("kg", vm.state.value.fromUnitId)
        assertNotNull(vm.state.value.category)
    }
}
