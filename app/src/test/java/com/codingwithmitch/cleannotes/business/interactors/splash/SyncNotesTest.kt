package com.codingwithmitch.cleannotes.business.interactors.splash

import com.codingwithmitch.cleannotes.business.data.cache.abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.model.NoteFactory
import com.codingwithmitch.cleannotes.business.domain.util.DateUtil
import com.codingwithmitch.cleannotes.di.DependencyContainer
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.collections.ArrayList

/*
Test cases:
1. insertNetworkNotesIntoCache()
    a) insert a bunch of new notes into the cache
    b) perform the sync
    c) check to see that those notes were inserted into the network
2. insertCachedNotesIntoNetwork()
    a) insert a bunch of new notes into the network
    b) perform the sync
    c) check to see that those notes were inserted into the cache
3. checkCacheUpdateLogicSync()
    a) select some notes from the cache and update them
    b) perform sync
    c) confirm network reflects the updates
4. checkNetworkUpdateLogicSync()
    a) select some notes from the network and update them
    b) perform sync
    c) confirm cache reflects the updates
 */
@InternalCoroutinesApi
class SyncNotesTest() {
    // system in test
    private val syncNotes: SyncNotes

    // dependencies
    private val dependencyContainer: DependencyContainer = DependencyContainer()
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory
    private val dateUtil: DateUtil

    init {
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        dateUtil = dependencyContainer.dateUtil

        syncNotes = SyncNotes(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    /**
     * Simulate this scenario (it was created to test a previous bug in the course):
     *
     * Device 1: open the app, update a note, close the app.
     * Device 2: open the app, it should sync and show the update, close the app.
     *           open the app again, it should not update the note.
     *
     * What should happen: when you update the local cache from network, it should
     * use the network timestamp, not generate a new one as it was doing before (bug).
     */
    @Test
    fun doSuccessiveUpdatesOccur() = runBlocking {
        // **********
        // Device 1

        // update a single note with new timestamp
        val newDate = dateUtil.getCurrentTimestamp()
        val updatedNote = Note(
            id = noteNetworkDataSource.getAllNotes().get(0).id,
            title = noteNetworkDataSource.getAllNotes().get(0).title,
            body = noteNetworkDataSource.getAllNotes().get(0).body,
            created_at = noteNetworkDataSource.getAllNotes().get(0).created_at,
            updated_at = newDate
        )
        noteNetworkDataSource.insertOrUpdateNote(updatedNote)

        syncNotes.syncNotes()

        delay(1001)

        // **********
        // Device 2

        // simulate launch app again
        syncNotes.syncNotes()

        // confirm the date was not updated a second time
        val notes = noteNetworkDataSource.getAllNotes()
        for (note in notes) {
            if (note.id.equals(updatedNote.id)) {
                assertTrue { note.updated_at.equals(newDate) }
            }
        }
    }

    @Test
    fun checkUpdatedAtDates() = runBlocking {

        // update a single note with new timestamp
        val newDate = dateUtil.getCurrentTimestamp()
        val updatedNote = Note(
            id = noteNetworkDataSource.getAllNotes().get(0).id,
            title = noteNetworkDataSource.getAllNotes().get(0).title,
            body = noteNetworkDataSource.getAllNotes().get(0).body,
            created_at = noteNetworkDataSource.getAllNotes().get(0).created_at,
            updated_at = newDate
        )
        noteNetworkDataSource.insertOrUpdateNote(updatedNote)

//        for (note in noteNetworkDataSource.getAllNotes()) {
//            println("date: ${note.updated_at}")
//        }
//        println("BREAK")

        syncNotes.syncNotes()

        // confirm only a single 'updated_at' date was updated
        val notes = noteNetworkDataSource.getAllNotes()
        for (note in notes) {
            noteCacheDataSource.searchNoteById(note.id)?.let { n ->
                println("date: ${n.updated_at}")
                if (n.id.equals(updatedNote.id)) {
                    assertTrue { n.updated_at.equals(newDate) }
                } else {
                    assertFalse { n.updated_at.equals(newDate) }
                }
            }
        }
    }


    @Test
    fun insertNetworkNotesIntoCache() = runBlocking {
        val newNotes = noteFactory.createNoteList(50)
        noteNetworkDataSource.insertOrUpdateNotes(newNotes)

        syncNotes.syncNotes()

        newNotes.forEach { note ->
            val cachedNote = noteCacheDataSource.searchNoteById(note.id)
            assertNotNull(cachedNote)
        }
    }

    @Test
    fun insertCachedNotesIntoNetwork() = runBlocking {
        val newNotes = noteFactory.createNoteList(50)
        noteCacheDataSource.insertNotes(newNotes)

        syncNotes.syncNotes()

        newNotes.forEach { note ->
            val networkNote = noteNetworkDataSource.searchNote(note)
            assertNotNull(networkNote)
        }
    }

    @Test
    fun checkCacheUpdateLogicSync() = runBlocking {
        val cachedNotes = noteCacheDataSource.searchNotes("", "", 1)
        val notesToUpdate = ArrayList<Note>()

        for (note in cachedNotes) {
            val updatedNote = noteFactory.createSingleNote(
                id = note.id, // id can't change, it is just an update
                title = UUID.randomUUID().toString(),
                body = UUID.randomUUID().toString()
            )
            notesToUpdate.add(updatedNote)

            // break the loop after some notes
            if (notesToUpdate.size > 4) {
                break
            }
        }
        // as the changes notes already exist, they will be updated into cache
        noteCacheDataSource.insertNotes(notesToUpdate)

        // The use case in test runs
        syncNotes.syncNotes()

        // confirm network reflects the updates
        for (note in notesToUpdate) {
            val networkNote = noteNetworkDataSource.searchNote(note)
            assertEquals(note.id, networkNote?.id)
            assertEquals(note.title, networkNote?.title)
            assertEquals(note.body, networkNote?.body)
            assertEquals(note.updated_at, networkNote?.updated_at)
        }
    }

    @Test
    fun checkNetworkUpdateLogicSync() = runBlocking {
        val networkNotes = noteNetworkDataSource.getAllNotes()
        val notesToUpdate = ArrayList<Note>()

        for (note in networkNotes) {
            val updatedNote = noteFactory.createSingleNote(
                id = note.id, // id can't change, it is just an update
                title = UUID.randomUUID().toString(),
                body = UUID.randomUUID().toString()
            )
            notesToUpdate.add(updatedNote)

            // break the loop after some notes
            if (notesToUpdate.size > 4) {
                break
            }
        }
        // Update notes into network
        noteNetworkDataSource.insertOrUpdateNotes(notesToUpdate)

        // The use case in test runs
        syncNotes.syncNotes()

        // confirm network reflects the updates
        for (note in notesToUpdate) {
            val cachedNote = noteCacheDataSource.searchNoteById(note.id)
            assertEquals(note.id, cachedNote?.id)
            assertEquals(note.title, cachedNote?.title)
            assertEquals(note.body, cachedNote?.body)
            assertEquals(note.updated_at, cachedNote?.updated_at)
        }
    }
}