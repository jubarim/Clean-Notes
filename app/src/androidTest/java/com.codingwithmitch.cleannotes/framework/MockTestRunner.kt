package com.codingwithmitch.cleannotes.framework

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.codingwithmitch.cleannotes.framework.presentation.TestBaseApplication

/**
 * To change runner to android junit runner (instead of default junit runner)
 */
class MockTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TestBaseApplication::class.java.name, context)
    }
}