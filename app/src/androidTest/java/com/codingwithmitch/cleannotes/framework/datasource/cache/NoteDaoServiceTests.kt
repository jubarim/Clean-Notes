package com.codingwithmitch.cleannotes.framework.datasource.cache

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.util.DateUtil
import com.codingwithmitch.cleannotes.di.TestAppComponent
import com.codingwithmitch.cleannotes.framework.BaseTest
import com.codingwithmitch.cleannotes.framework.datasource.cache.abstraction.NoteDaoService
import com.codingwithmitch.cleannotes.framework.datasource.cache.database.NoteDao
import com.codingwithmitch.cleannotes.framework.datasource.cache.implementation.NoteDaoServiceImpl
import com.codingwithmitch.cleannotes.framework.datasource.cache.mappers.CacheMapper
import com.codingwithmitch.cleannotes.framework.datasource.data.NoteDataFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// runBlockingTest doesn't work:
// https://github.com/Kotlin/kotlinx.coroutines/issues/1204

/*
    LEGEND:
    1. CBS = "Confirm by searching"

    Test cases:
    1. confirm database not empty to start (should be test data inserted from CacheTest.kt)
    2. insert a new note, CBS
    3. insert a list of notes, CBS
    4. insert 1000 new notes, confirm filtered search query works correctly
    5. insert 1000 new notes, confirm db size increased
    6. delete new note, confirm deleted
    7. delete list of notes, CBS
    8. update a note, confirm updated
    9. search notes, order by date (ASC), confirm order
    10. search notes, order by date (DESC), confirm order
    11. search notes, order by title (ASC), confirm order
    12. search notes, order by title (DESC), confirm order
 */
@RunWith(AndroidJUnit4ClassRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class NoteDaoServiceTests : BaseTest() {

    // system in test
    private val noteDaoService: NoteDaoService

    // dependencies
    @Inject
    lateinit var dao: NoteDao

    @Inject
    lateinit var noteFactory: NoteDataFactory

    @Inject
    lateinit var dateUtil: DateUtil

    @Inject
    lateinit var cacheMapper: CacheMapper

    init {
        injectTest()
        insertTestData()
        noteDaoService = NoteDaoServiceImpl(
            noteDao = dao,
            noteMapper = cacheMapper,
            dateUtil = dateUtil
        )
    }

    private fun insertTestData() = runBlocking {
        val entityList = cacheMapper.noteListToEntityList(
            noteFactory.produceListOfNotes()
        )

        dao.insertNotes(entityList)
    }

    // 1. confirm database not empty to start (should be test data inserted from NoteDataFactory.kt)
    @Test
    fun a_searchNotes_confirmDbNotEmpty() = runBlocking {
        val numNotes = noteDaoService.getNumNotes()

        assertTrue { numNotes > 0 }
    }

    // 2. insert a new note, CBS
    @Test
    fun insertNote_CBS() = runBlocking {
        val newNote = noteFactory.createSingleNote(
            null,
            "Super cool title",
            "Some content for the note"
        )
        noteDaoService.insertNote(newNote)

        // CBS
        val notes = noteDaoService.searchNotes()
        assert(notes.contains(newNote))
    }

    // 3. insert a list of notes, CBS
    @Test
    fun insertNoteList_CBS() = runBlocking {
        val noteList = noteFactory.createNoteList(10)
        noteDaoService.insertNotes(noteList)

        val queriedNotes = noteDaoService.searchNotes()
        assert(queriedNotes.containsAll(noteList))
    }

    // 4. insert 1000 new notes, confirm filtered search query works correctly
    @Test
    fun insert1000Notes_searchNotesByTitle_confirm50ExpectedValues() = runBlocking {
        val noteList = noteFactory.createNoteList(1000)
        noteDaoService.insertNotes(noteList)

        repeat(50) {
            val randomIndex = Random.nextInt(0, noteList.size - 1)

            // query just a specific note - just one result
            val result = noteDaoService.searchNotesOrderByTitleASC(
                query = noteList[randomIndex].title,
                page = 1,
                pageSize = 1
            )

            assertEquals(
                noteList[randomIndex].title, result.get(0).title
            )
        }
    }

    // 5. insert 1000 new notes, confirm db size increased
    @Test
    fun insert1000Notes_confirmNumNotesInDb() = runBlocking {
        val currentNumNotes = noteDaoService.getNumNotes()

        val noteList = noteFactory.createNoteList(1000)
        noteDaoService.insertNotes(noteList)

        val newNumNotes = noteDaoService.getNumNotes()

        assertEquals(currentNumNotes + 1000, newNumNotes)
    }

    // 6. delete new note, confirm deleted
    @Test
    fun insertNote_deleteNote_confirmDeleted() = runBlocking {
        val newNote = noteFactory.createSingleNote(
            null,
            "Super cool title 2",
            "Some content for the note by JM"
        )
        noteDaoService.insertNote(newNote)

        var notes = noteDaoService.getAllNotes()
        assert(notes.contains(newNote))

        noteDaoService.deleteNote(newNote.id)

        // CBS
        notes = noteDaoService.getAllNotes()
        assertFalse { notes.contains(newNote) }

    }

    // 7. delete list of notes, CBS
    @Test
    fun deleteNoteList_confirmDeleted() = runBlocking {
        val noteList = ArrayList(noteDaoService.searchNotes())

        // get 4 notes to delete
        val notesToDelete: ArrayList<Note> = ArrayList()
        repeat(4) {
            val noteToDelete = noteList[Random.nextInt(0, noteList.size - 1) + 1]
            noteList.remove(noteToDelete)
            notesToDelete.add(noteToDelete)
        }

        noteDaoService.deleteNotes(notesToDelete)

        // confirm they were deleted
        val searchResults = noteDaoService.searchNotes()
        assertFalse { searchResults.containsAll(notesToDelete) }
    }

    // 8. update a note, confirm updated
    @Test
    fun insertNote_updateNote_confirmUpdated() = runBlocking {
        val newNote = noteFactory.createSingleNote(
            null,
            "Super cool title 2",
            "Some content for the note by JM"
        )
        noteDaoService.insertNote(newNote)

        // Update note after 1.1 sec - so updated date is greater than newNotes'
        delay(1100)
        val newTitle = UUID.randomUUID().toString()
        val newBody = UUID.randomUUID().toString()
        noteDaoService.updateNote(
            primaryKey = newNote.id,
            title = newTitle,
            body = newBody,
            timestamp = null
        )

        val notes = noteDaoService.searchNotes()

        var foundNote = false
        for (note in notes) {
            if (note.id == newNote.id) {
                println("jm_ newNote.updated_at=${newNote.updated_at}")
                println("jm_ note   .updated_at=${note.updated_at}")

                foundNote = true
                assertEquals(newNote.id, note.id)
                assertEquals(newTitle, note.title)
                assertEquals(newBody, note.body)
                assert(newNote.updated_at != note.updated_at) // should have changed!
                assertEquals(newNote.created_at, note.created_at)
                break
            }
        }

        assertTrue { foundNote }
    }

    // 9. search notes, order by date (ASC), confirm order
    @Test
    fun searchNotes_orderByDateASC_confirmOrder() = runBlocking {
        val noteList = noteDaoService.searchNotesOrderByDateASC(
            query = "",
            page = 1,
            pageSize = 100
        )

        var previousNoteDate = noteList[0].updated_at
        for (index in 1 until noteList.size) {
            val currentNoteDate = noteList[index].updated_at
            assertTrue {
                currentNoteDate >= previousNoteDate
            }
            previousNoteDate = currentNoteDate
        }
    }

    // 10. search notes, order by date (DESC), confirm order
    @Test
    fun searchNotes_orderByDateDESC_confirmOrder() = runBlocking {
        val noteList = noteDaoService.searchNotesOrderByDateDESC(
            query = "",
            page = 1,
            pageSize = 100
        )

        // check that the date gets larger (newer) as iterate down the list
        var previous = noteList[0].updated_at
        for (index in 1 until noteList.size) {
            val current = noteList[index].updated_at
            assertTrue { current <= previous }
            previous = current
        }

    }

    // 11. search notes, order by title (ASC), confirm order
    @Test
    fun searchNotes_orderByTitleASC_confirmOrder() = runBlocking {
        val noteList = noteDaoService.searchNotesOrderByTitleASC(
            query = "",
            page = 1,
            pageSize = 100
        )

        // check that the date gets larger (newer) as iterate down the list
        var previous = noteList[0].title
        for (index in 1 until noteList.size) {
            val current = noteList[index].title

            assertTrue {
                listOf(previous, current)
                    .asSequence()
                    .zipWithNext { a, b ->
                        a <= b
                    }.all { it }
            }
            previous = current
        }
    }

    // 12. search notes, order by title (DESC), confirm order
    @Test
    fun searchNotes_orderByTitleDESC_confirmOrder() = runBlocking {
        val noteList = noteDaoService.searchNotesOrderByTitleDESC(
            query = "",
            page = 1,
            pageSize = 100
        )

        // check that the date gets larger (newer) as iterate down the list
        var previous = noteList[0].title
        for (index in 1 until noteList.size) {
            val current = noteList[index].title

            assertTrue {
                listOf(previous, current)
                    .asSequence()
                    .zipWithNext { a, b ->
                        a >= b
                    }.all { it }
            }
            previous = current
        }
    }

    override fun injectTest() {
        (application.appComponent as TestAppComponent).inject(this)
    }

}