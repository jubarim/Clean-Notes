package com.codingwithmitch.cleannotes.business.interactors.notelist

import com.codingwithmitch.cleannotes.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.codingwithmitch.cleannotes.business.data.cache.FORCE_GENERAL_FAILURE
import com.codingwithmitch.cleannotes.business.data.cache.FORCE_NEW_NOTE_EXCEPTION
import com.codingwithmitch.cleannotes.business.data.cache.abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.domain.model.NoteFactory
import com.codingwithmitch.cleannotes.business.domain.state.DataState
import com.codingwithmitch.cleannotes.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_FAILED
import com.codingwithmitch.cleannotes.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_SUCCESS
import com.codingwithmitch.cleannotes.di.DependencyContainer
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListStateEvent
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListStateEvent.RestoreDeletedNoteEvent
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/*
Test cases:
1. restoreNote_success_confirmCacheAndNetworkUpdated()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note
    c) Listen for success msg RESTORE_NOTE_SUCCESS from flow
    d) confirm note is in the cache
    e) confirm note is in the network "notes" node
    f) confirm note is not in the network "deletes" node
2. restoreNote_fail_confirmCacheAndNetworkUnchanged()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note (force a failure)
    c) Listen for success msg RESTORE_NOTE_FAILED from flow
    d) confirm note is not in the cache
    e) confirm note is not in the network "notes" node
    f) confirm note is in the network "deletes" node
3. throwException_checkGenericError_confirmNetworkAndCacheUnchanged()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note (force an exception)
    c) Listen for success msg CACHE_ERROR_UNKNOWN from flow
    d) confirm note is not in the cache
    e) confirm note is not in the network "notes" node
    f) confirm note is in the network "deletes" node
 */
@InternalCoroutinesApi
class RestoreDeletedNoteTest {

    // system in test
    private val restoreDeletedNote: RestoreDeletedNote

    // dependencies
    private val dependencyContainer: DependencyContainer
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        restoreDeletedNote = RestoreDeletedNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    fun restoreNote_success_confirmCacheAndNetworkUpdated() = runBlocking {
        // Simulate a deleted operation occurred - a note is not in the cache
        // but exists in the deleted notes node

        // Create a new note and insert into the 'deletes' note
        val restoredNote = noteFactory.createSingleNote(
            id = "1039841304daakdd",
            title = "1039841304daakdd",
            body = "1039841304daakdd"
        )
        noteNetworkDataSource.insertDeletedNote(restoredNote)

        // confirm that note is in the 'deletes' node before restoration
        // (optional)
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue { deletedNotes.contains(restoredNote) }

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = RestoreDeletedNoteEvent(restoredNote)
        ).collect {
            object : FlowCollector<DataState<NoteListViewState>?> {
                override suspend fun emit(value: DataState<NoteListViewState>?) {

                    assertEquals(
                        value?.stateMessage?.response?.message, RESTORE_NOTE_SUCCESS
                    )
                }
            }
        }

        // confirm note is in the cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertTrue { noteInCache == restoredNote }

        // confirm note is in the network 'notes' node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertTrue { noteInNetwork == restoredNote }

        // confirm note is NOT in the network 'deletes' node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertFalse { deletedNotes.contains(restoredNote) }
    }

    @Test
    fun restoreNote_fail_confirmCacheAndNetworkUnchanged() = runBlocking {

        // Create a new note to force failure
        val restoredNote = noteFactory.createSingleNote(
            id = FORCE_GENERAL_FAILURE,
            title = "1039841304daakdd",
            body = "1039841304daakdd"
        )
        noteNetworkDataSource.insertDeletedNote(restoredNote)

        // confirm that note is in the 'deletes' node before restoration
        // (optional)
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue { deletedNotes.contains(restoredNote) }

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = RestoreDeletedNoteEvent(restoredNote)
        ).collect {
            object : FlowCollector<DataState<NoteListViewState>?> {
                override suspend fun emit(value: DataState<NoteListViewState>?) {

                    assertEquals(
                        value?.stateMessage?.response?.message, RESTORE_NOTE_FAILED
                    )
                }
            }
        }

        // confirm note is NOT in the cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertFalse { noteInCache == restoredNote }

        // confirm note is NOT in the network 'notes' node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertFalse { noteInNetwork == restoredNote }

        // confirm note is IN the network 'deletes' node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue { deletedNotes.contains(restoredNote) }
    }

    @Test
    fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {
        // Create a new note that forces generic error exception
        val restoredNote = noteFactory.createSingleNote(
            id = FORCE_NEW_NOTE_EXCEPTION,
            title = "1039841304daakdd",
            body = "1039841304daakdd"
        )
        noteNetworkDataSource.insertDeletedNote(restoredNote)

        // confirm that note is in the 'deletes' node before restoration
        // (optional)
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue { deletedNotes.contains(restoredNote) }

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = RestoreDeletedNoteEvent(restoredNote)
        ).collect {
            object : FlowCollector<DataState<NoteListViewState>?> {
                override suspend fun emit(value: DataState<NoteListViewState>?) {

                    assert(
                        value?.stateMessage?.response?.message?.contains(
                            CACHE_ERROR_UNKNOWN
                        ) ?: false
                    )
                }
            }
        }

        // confirm note is NOT in the cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertFalse { noteInCache == restoredNote }

        // confirm note is NOT in the network 'notes' node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertFalse { noteInNetwork == restoredNote }

        // confirm note is IN the network 'deletes' node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue { deletedNotes.contains(restoredNote) }
    }
}