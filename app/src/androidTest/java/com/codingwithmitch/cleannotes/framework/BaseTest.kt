package com.codingwithmitch.cleannotes.framework

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.codingwithmitch.cleannotes.framework.presentation.TestBaseApplication

abstract class BaseTest {

    // dependencies
    val application: TestBaseApplication =
        ApplicationProvider.getApplicationContext<Context>() as TestBaseApplication

    abstract fun injectTest()
}
