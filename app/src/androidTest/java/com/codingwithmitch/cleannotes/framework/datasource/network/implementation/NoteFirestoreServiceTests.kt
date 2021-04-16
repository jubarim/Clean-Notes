package com.codingwithmitch.cleannotes.framework.datasource.network.implementation

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.di.TestAppComponent
import com.codingwithmitch.cleannotes.framework.BaseTest
import com.codingwithmitch.cleannotes.framework.datasource.data.NoteDataFactory
import com.codingwithmitch.cleannotes.framework.datasource.network.abstraction.NoteFirestoreService
import com.codingwithmitch.cleannotes.framework.datasource.network.implementation.NoteFirestoreServiceImpl.Companion.NOTES_COLLECTION
import com.codingwithmitch.cleannotes.framework.datasource.network.implementation.NoteFirestoreServiceImpl.Companion.USER_ID
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
import kotlin.random.Random
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Note: remember to start emulator in a terminal by typing:
 * ❯ firebase emulators:start --only firestore
 */
/*
LEGEND:
 CBS = "Confirm by searching"
Test cases:
1. insert a single note, CBS
2. update a random note, CBS
3. insert a list of notes, CBS
4. delete a single note, CBS
5. insert a deleted note into "deletes" node, CBS
6. insert a list of deleted notes into "deletes" node, CBS
7. delete a 'deleted note' (note from "deletes" node). CBS
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
    lateinit var noteDataFactory: NoteDataFactory

    @Inject
    lateinit var networkMapper: NetworkMapper

    init {
        injectTest()
        signIn()
        insertTestData()
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

    private fun insertTestData() {
        val entityList = networkMapper.noteListToEntityList(
            noteDataFactory.produceListOfNotes()
        )

        for (entity in entityList) {
            firestore
                .collection(NOTES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .document(entity.id)
                .set(entity)
        }
    }

    @Test
    fun insertSingleNote_CBS() = runBlocking {
        val note = noteDataFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        noteFirestoreService.insertOrUpdateNote(note)

        // Check note was correctly inserted
        val searchResult = noteFirestoreService.searchNote(note)
        assertEquals(note, searchResult)
    }

    @Test
    fun updateSingleNote_CBS() = runBlocking {
        val searchResults = noteFirestoreService.getAllNotes()

        // choose a random note from list and update it
        val randomNote = getRandomNote(searchResults)
        val UPDATED_TITLE = UUID.randomUUID().toString()
        val UPDATED_BODY = UUID.randomUUID().toString()
        var updatedNote = noteDataFactory.createSingleNote(
            id = randomNote.id,
            title = UPDATED_TITLE,
            body = UPDATED_BODY
        )

        // make the update
        noteFirestoreService.insertOrUpdateNote(updatedNote)

        // query note and check the values - we force here to not nullable
        updatedNote = noteFirestoreService.searchNote(updatedNote)!!

        assertEquals(UPDATED_TITLE, updatedNote.title)
        assertEquals(UPDATED_BODY, updatedNote.body)
    }

    @Test
    fun insertNoteList_CBS() = runBlocking {
        val list = noteDataFactory.createNoteList(50)

        noteFirestoreService.insertOrUpdateNotes(list)

        val searchResults = noteFirestoreService.getAllNotes()

        // Confirm results contains all inserted notes
        assertTrue { searchResults.containsAll(list) }
    }

    @Test
    fun deleteSingleNote_CBS() = runBlocking {
        val noteList = noteFirestoreService.getAllNotes()

        val noteToDelete = getRandomNote(noteList)

        noteFirestoreService.deleteNote(noteToDelete.id)

        // Confirm note was deleted from 'Notes' node
        val firestoreNotes = noteFirestoreService.getAllNotes()
        assertFalse { firestoreNotes.contains(noteToDelete) }
    }

    @Test
    fun insertIntoDeletesNode_CBS() = runBlocking {
        val noteList = noteFirestoreService.getAllNotes()

        // choose a random note from list and insert into deletes node
        val noteToDelete = getRandomNote(noteList)

        noteFirestoreService.insertDeletedNote(noteToDelete)

        // Confirm it is now in the deletes node
        val searchResults = noteFirestoreService.getDeletedNotes()

        assertTrue { searchResults.contains(noteToDelete) }
    }

    @Test
    fun insertListIntoDeletesNode() = runBlocking {
        val noteList = ArrayList(noteFirestoreService.getAllNotes())

        // choose some random notes do add to 'deletes' node
        val notesToDelete: ArrayList<Note> = ArrayList()
        for (i in 1..4) {
            val note = getRandomNote(noteList)
            notesToDelete.add(note)
            noteList.remove(note)
        }

        noteFirestoreService.insertDeletedNotes(notesToDelete)

        // Confirm results contains all deleted notes (inserted into deletes node)
        val searchResults = noteFirestoreService.getDeletedNotes()
        assertTrue { searchResults.containsAll(notesToDelete) }
    }

    @Test
    fun deleteDeletedNote_CBS() = runBlocking {
        val note = noteDataFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        // Insert into 'deletes' node
        noteFirestoreService.insertDeletedNote(note)

        // Confirm note is in 'deletes' node
        var searchResults = noteFirestoreService.getDeletedNotes()
        assertTrue { searchResults.contains(note) }

        // delete from 'deletes' node
        noteFirestoreService.deleteDeletedNote(note)

        // Confirm note is no longer in 'deletes' node
        searchResults = noteFirestoreService.getDeletedNotes()
        assertFalse { searchResults.contains(note) }
    }

    private fun getRandomNote(list: List<Note>) =
        list[Random.nextInt(0, list.size - 1) + 1]


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
