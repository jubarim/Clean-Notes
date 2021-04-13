package com.codingwithmitch.cleannotes.di

import com.codingwithmitch.cleannotes.business.TempTest
import com.codingwithmitch.cleannotes.framework.presentation.TestBaseApplication
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        TestModule::class
    ]
)
interface TestAppComponent : AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance app: TestBaseApplication): TestAppComponent
    }

    fun inject(tempTest: TempTest)
}
