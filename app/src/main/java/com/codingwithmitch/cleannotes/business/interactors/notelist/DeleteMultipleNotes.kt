package com.codingwithmitch.cleannotes.business.interactors.notelist

import com.codingwithmitch.cleannotes.business.data.cache.CacheResponseHandler
import com.codingwithmitch.cleannotes.business.data.cache.abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.data.util.safeApiCall
import com.codingwithmitch.cleannotes.business.data.util.safeCacheCall
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.state.*
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeleteMultipleNotes(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {

    private var onDeleteError = false

    fun deleteNotes(
        notes: List<Note>,
        stateEvent: StateEvent
    ): Flow<DataState<NoteListViewState>?> = flow {

        // To update remote (firestore) correctly later, we need to keep track
        // of correctly deleted notes
        val successfulDeletes: ArrayList<Note> = ArrayList()

        notes.forEach { note ->

            val cacheResult = safeCacheCall(IO) {
                noteCacheDataSource.deleteNote(note.id)
            }

            val response = object : CacheResponseHandler<NoteListViewState, Int>(
                response = cacheResult,
                stateEvent = stateEvent
            ) {
                override suspend fun handleSuccess(resultObj: Int): DataState<NoteListViewState>? {
                    if (resultObj < 0) {
                        // Error case
                        onDeleteError = true
                    } else {
                        // Success
                        successfulDeletes.add(note)
                    }
                    return null
                }
            }.getResult()

            // There is a chance for another error when calling deleteNote and in that case, the
            // handleSuccess is not called, so we need to check it here.
            if (response?.stateMessage?.response?.message
                    ?.contains(stateEvent.errorInfo()) == true) {
                onDeleteError = true
            }

            // If an error occurred, notify
            if (onDeleteError) {
                emit(
                    DataState.data<NoteListViewState>(
                        response = Response(
                            message = DELETE_NOTES_ERRORS,
                            uiComponentType = UIComponentType.Dialog(),
                            messageType = MessageType.Success()             // why success?? jm_
                        ),
                        data = null,
                        stateEvent = stateEvent
                    )
                )
            } else {
                emit(
                    DataState.data<NoteListViewState>(
                        response = Response(
                            message = DELETE_NOTES_SUCCESS,
                            uiComponentType = UIComponentType.Toast(),
                            messageType = MessageType.Success()             // why success?? jm_
                        ),
                        data = null,
                        stateEvent = stateEvent
                    )
                )
            }

            updateNetwork(successfulDeletes)
        }


    }

    // TODO: improvement - check if Firestore supports bulk delete / insert
    private suspend fun updateNetwork(successfulDeletes: ArrayList<Note>) {
        successfulDeletes.forEach { note ->
            // delete from 'notes' node
            safeApiCall(IO) {
                noteNetworkDataSource.deleteNote(note.id)
            }

            safeApiCall(IO) {
                noteNetworkDataSource.insertDeletedNote(note)
            }
        }
    }

    companion object{
        const val DELETE_NOTES_SUCCESS = "Successfully deleted notes."
        const val DELETE_NOTES_ERRORS = "Not all the notes you selected were deleted. There was some errors."
        const val DELETE_NOTES_YOU_MUST_SELECT = "You haven't selected any notes to delete."
        const val DELETE_NOTES_ARE_YOU_SURE = "Are you sure you want to delete these?"
    }

}
