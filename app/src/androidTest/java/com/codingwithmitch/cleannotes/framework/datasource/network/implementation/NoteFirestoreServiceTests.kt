package com.codingwithmitch.cleannotes.framework.datasource.network.implementation

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.codingwithmitch.cleannotes.business.domain.model.NoteFactory
import com.codingwithmitch.cleannotes.di.TestAppComponent
import com.codingwithmitch.cleannotes.framework.BaseTest
import com.codingwithmitch.cleannotes.framework.datasource.network.abstraction.NoteFirestoreService
import com.codingwithmitch.cleannotes.framework.datasource.network.mappers.NetworkMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import javax.inject.Inject
import kotlin.test.assertEquals

/**
 * Note: remember to start emulator in a terminal by typing:
 * ❯ firebase emulators:start --only firestore
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class NoteFirestoreServiceTests : BaseTest() {

    // system in test
    private lateinit var noteFirestoreService: NoteFirestoreService

    @Inject
    lateinit var firestore: FirebaseFirestore // we have injected here the local emulator firestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var noteFactory: NoteFactory

    @Inject
    lateinit var networkMapper: NetworkMapper

    init {
        injectTest()
        signIn()
    }

    @Before
    fun before() {
        noteFirestoreService = NoteFirestoreServiceImpl(
            firebaseAuth = FirebaseAuth.getInstance(),
            firestore = firestore,
            networkMapper = networkMapper
        )
    }

    private fun signIn() = runBlocking {
        firebaseAuth.signInWithEmailAndPassword(
            EMAIL,
            PASSWORD
        ).await()
    }

    @Test
    fun insertSingleNote_CBS() = runBlocking {
        val note = noteFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        noteFirestoreService.insertOrUpdateNote(note)

        // Check note was correctly inserted
        val searchResult = noteFirestoreService.searchNote(note)
        assertEquals(note, searchResult)
    }

    companion object {
        // just for testing
        const val EMAIL = "jubarim@gmail.com"
        const val PASSWORD = "8jmarc"
    }

    override fun injectTest() {
        (application.appComponent as TestAppComponent)
            .inject(this)
    }
}
