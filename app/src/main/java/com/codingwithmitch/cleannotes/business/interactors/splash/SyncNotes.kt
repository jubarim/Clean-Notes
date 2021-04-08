package com.codingwithmitch.cleannotes.business.interactors.splash

import com.codingwithmitch.cleannotes.business.data.cache.CacheResponseHandler
import com.codingwithmitch.cleannotes.business.data.cache.abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.network.ApiResponseHandler
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.data.util.safeApiCall
import com.codingwithmitch.cleannotes.business.data.util.safeCacheCall
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.state.DataState
import com.codingwithmitch.cleannotes.util.printLogD
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
    Query all notes in the cache. It will then search firestore for
    each corresponding note but with an extra filter: It will only return notes where
    cached_note.updated_at < network_note.updated_at. It will update the cached notes
    where that condition is met. If the note does not exist in Firestore (maybe due to
    network being down at time of insertion), insert it
    (**This must be done AFTER
    checking for deleted notes and performing that sync**).
 */
class SyncNotes(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {
    suspend fun syncNotes() {
        val cachedNotesList = getCachedNotes()

        syncNetworkNotesWithCachedNotes(ArrayList(cachedNotesList))
    }

    private suspend fun getCachedNotes(): List<Note> {

        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.getAllNotes()
        }

        val response = object : CacheResponseHandler<List<Note>, List<Note>>(
            response = cacheResult,
            stateEvent = null
        ) {
            override suspend fun handleSuccess(resultObj: List<Note>): DataState<List<Note>> {
                // We just need the data
                return DataState.data(
                    response = null,
                    data = resultObj,
                    stateEvent = null
                )
            }
        }.getResult()

        return response?.data ?: ArrayList()
    }

    // Get all notes from network
    // if they do not exist in cache, insert them
    // if they do exist in cache, make sure they are up to date
    // while looping, remove notes from the cachedNotes list. If any remain, it means they
    // should be in the network but aren't. So insert them.
    private suspend fun syncNetworkNotesWithCachedNotes(
        cachedNotes: ArrayList<Note>
    ) = withContext(IO) {

        // Brute force way - as notes should not be very big - for big databases
        // a different strategy should be used
        val networkResult = safeApiCall(IO) {
            noteNetworkDataSource.getAllNotes()
        }

        val response = object : ApiResponseHandler<List<Note>, List<Note>>(
            response = networkResult,
            stateEvent = null
        ) {
            override suspend fun handleSuccess(resultObj: List<Note>): DataState<List<Note>> {
                // We just need the data
                return DataState.data(
                    response = null,
                    data = resultObj,
                    stateEvent = null
                )
            }
        }.getResult()

        val noteList = response?.data ?: ArrayList()

        val job = launch {
            // Loop thu network notes and do required processing
            noteList.forEach { note ->
                noteCacheDataSource.searchNoteById(note.id)?.let { cachedNote ->
                    // If a note already exists in our cache, remove from our list - this note needs no further processing
                    cachedNotes.remove(cachedNote)
                    // Just check if cached note needs to be updated
                    checkIfCachedNoteRequiresUpdate(cachedNote, note)
                }?: noteCacheDataSource.insertNote(note) // if note does not exist in cache, add it!
            }
        }
        // wait for the first job to finish
        job.join()

        // insert remaining into network
        cachedNotes.forEach { cachedNote ->
            safeApiCall(IO) {
                noteNetworkDataSource.insertOrUpdateNote(cachedNote)
            }
        }
    }

    private suspend fun checkIfCachedNoteRequiresUpdate(
        cachedNote: Note,
        networkNote: Note
    ) {
        val cacheUpdatedAt = cachedNote.updated_at
        val networkUpdatedAt = networkNote.updated_at

        printLogD(
            "SyncNotes",
            "cacheUpdatedAt: ${cacheUpdatedAt}, " +
                    "networkUpdatedAt: ${networkUpdatedAt}, " +
                    "note: ${cachedNote.title}"
        )

        // update cache (network has newest data)
        if (networkUpdatedAt > cacheUpdatedAt) {
            printLogD("SyncNotes", "Update cache")

            safeCacheCall(IO) {
                noteCacheDataSource.updateNote(
                    primaryKey = networkNote.id,
                    newTitle = networkNote.title,
                    newBody = networkNote.body
                )
            }
            // update network (cache has the newest data)
        } else {
            printLogD("SyncNotes", "Update network")

            safeApiCall(IO) {
                noteNetworkDataSource.insertOrUpdateNote(cachedNote)
            }
        }
    }
}
