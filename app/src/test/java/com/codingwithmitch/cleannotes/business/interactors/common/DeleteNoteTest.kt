package com.codingwithmitch.cleannotes.business.interactors.common

import com.codingwithmitch.cleannotes.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.codingwithmitch.cleannotes.business.data.cache.FORCE_DELETE_NOTE_EXCEPTION
import com.codingwithmitch.cleannotes.business.data.cache.abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.model.NoteFactory
import com.codingwithmitch.cleannotes.business.domain.state.DataState
import com.codingwithmitch.cleannotes.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_FAILED
import com.codingwithmitch.cleannotes.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.codingwithmitch.cleannotes.business.interactors.notelist.GetNumNotes
import com.codingwithmitch.cleannotes.di.DependencyContainer
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListStateEvent
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListStateEvent.DeleteNoteEvent
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/*
Test cases:
1. deleteNote_success_confirmNetworkUpdated()
    a) delete a note
    b) check for success message from flow emission
    c) confirm note was deleted from "notes" node in network
    d) confirm note was added to "deletes" node in network
2. deleteNote_fail_confirmNetworkUnchanged()
    a) attempt to delete a note, fail since does not exist
    b) check for failure message from flow emission
    c) confirm network was not changed
3. throwException_checkGenericError_confirmNetworkUnchanged()
    a) attempt to delete a note, force an exception to throw
    b) check for failure message from flow emission
    c) confirm network was not changed
 */
@InternalCoroutinesApi
class DeleteNoteTest {

    // system in test
    private val deleteNote: DeleteNote<NoteListViewState>

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

        deleteNote = DeleteNote(noteCacheDataSource, noteNetworkDataSource)
    }

    @Test
    fun deleteNote_success_confirmNetworkUpdated() = runBlocking {

        // choose a note at random to delete
        // select a random note to update
        val noteToDelete = noteCacheDataSource
            .searchNotes("", "", 1)[0]

        deleteNote.deleteNote(
            noteToDelete,
            DeleteNoteEvent(noteToDelete)
        ).collect(object: FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
                assertEquals(
                    value?.stateMessage?.response?.message,
                    DELETE_NOTE_SUCCESS
                )
            }
        })

        // confirm was deleted from "notes" node
        val wasNoteDeleted = !noteNetworkDataSource.getAllNotes()
            .contains(noteToDelete)
        assertTrue { wasNoteDeleted }

        // confirm was inserted into "deletes" node
        val wasDeletedNoteInserted = noteNetworkDataSource.getDeletedNotes()
            .contains(noteToDelete)
        assertTrue { wasDeletedNoteInserted }
    }

    @Test
    fun deleteNote_fail_confirmNetworkUnchanged() = runBlocking {

        // Create a note that does not exist in cache
        val noteToDelete = Note(
            id = "blafungasss",
            title = "Byganga",
            body = "xxxxxx",
            created_at = "2030303030",
            updated_at = "2039083204"
        )

        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = DeleteNoteEvent(noteToDelete)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?> {
            override suspend fun emit(value: DataState<NoteListViewState>?) {

                assertEquals(
                    value?.stateMessage?.response?.message,
                    DELETE_NOTE_FAILED
                )

                // confirm note was NOT deleted from "notes" node in network
                val notes = noteNetworkDataSource.getAllNotes()
                val numNotesInCache = noteCacheDataSource.getNumNotes()
                assertTrue { notes.size == numNotesInCache }

                // confirm note was NOT added to "deletes" node in network
                val wasDeletedNoteInserted = !noteNetworkDataSource.getDeletedNotes()
                    .contains(noteToDelete)
                assertTrue { wasDeletedNoteInserted }

            }
        })
    }

    @Test
    fun throwException_checkGenericError_confirmNetworkUnchanged() = runBlocking {

        // Create a note to force exception in our mock system
        val noteToDelete = Note(
            id = FORCE_DELETE_NOTE_EXCEPTION,
            title = "Byganga",
            body = "xxxxxx",
            created_at = "2030303030",
            updated_at = "2039083204"
        )

        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = DeleteNoteEvent(noteToDelete)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?> {
            override suspend fun emit(value: DataState<NoteListViewState>?) {

                assert(
                    value?.stateMessage?.response?.message
                        ?.contains(CACHE_ERROR_UNKNOWN) ?: false
                )

                // confirm note was NOT deleted from "notes" node in network
                val notes = noteNetworkDataSource.getAllNotes()
                val numNotesInCache = noteCacheDataSource.getNumNotes()
                assertTrue { notes.size == numNotesInCache }

                // confirm note was NOT added to "deletes" node in network
                val wasDeletedNoteInserted = !noteNetworkDataSource.getDeletedNotes()
                    .contains(noteToDelete)
                assertTrue { wasDeletedNoteInserted }

            }
        })
    }

}
