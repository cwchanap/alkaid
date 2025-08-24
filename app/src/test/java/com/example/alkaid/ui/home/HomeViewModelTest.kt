package com.example.alkaid.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `default text is set`() {
        val vm = HomeViewModel()
        assertEquals("This is home Fragment", vm.text.value)
    }
}

