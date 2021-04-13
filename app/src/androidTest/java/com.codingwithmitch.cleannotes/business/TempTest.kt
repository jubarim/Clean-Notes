package com.codingwithmitch.cleannotes.business

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.codingwithmitch.cleannotes.di.TestAppComponent
import com.codingwithmitch.cleannotes.framework.presentation.TestBaseApplication
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4ClassRunner::class)
class TempTest {

    private val application: TestBaseApplication
        = ApplicationProvider.getApplicationContext() as TestBaseApplication

    @Inject
    lateinit var firebaseFirestore: FirebaseFirestore

    init {
        (application.appComponent as TestAppComponent)
            .inject(this)
    }

    @Test
    fun someRandomTest() {
        assert(::firebaseFirestore.isInitialized)
    }
}
