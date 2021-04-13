package com.codingwithmitch.cleannotes.framework.presentation

import com.codingwithmitch.cleannotes.di.DaggerTestAppComponent

class TestBaseApplication : BaseApplication() {
    override fun initAppComponent() {
        appComponent = DaggerTestAppComponent.factory()
            .create(this)
    }
}