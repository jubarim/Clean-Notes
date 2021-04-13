package com.codingwithmitch.cleannotes.framework.datasource.network.implementation

import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.framework.datasource.network.abstraction.NoteFirestoreService
import com.codingwithmitch.cleannotes.framework.datasource.network.mappers.NetworkMapper
import com.codingwithmitch.cleannotes.framework.datasource.network.model.NoteNetworkEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteFirestoreServiceImpl
@Inject
constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMapper: NetworkMapper
) : NoteFirestoreService {

    override suspend fun insertOrUpdateNote(note: Note) {
        val entity = getEntity(note)

        firestore
            .collection(NOTES_COLLECTION)
            .document(USER_ID)
            .collection(NOTES_COLLECTION)
            .document(entity.id)
            .set(entity)
            .await() // convert to a suspend function, so it waits in our usecases
    }

    override suspend fun insertOrUpdateNotes(notes: List<Note>) {
        if (notes.size > 500) {
            throw Exception("Cannot insert more than 500 notes at a time into firestore.")
        }

        val collectionRef = firestore
            .collection(NOTES_COLLECTION)
            .document(USER_ID)
            .collection(NOTES_COLLECTION)

        firestore.runBatch { batch ->
            for (note in notes) {
                val entity = getEntity(note)
                val documentRef = collectionRef.document(note.id)
                batch.set(documentRef, entity)
            }
        }
    }

    override suspend fun deleteNote(primaryKey: String) {
        firestore
            .collection(NOTES_COLLECTION)
            .document(USER_ID)
            .collection(NOTES_COLLECTION)
            .document(primaryKey)
            .delete()
            .await()
    }

    override suspend fun insertDeletedNote(note: Note) {
        val entity = networkMapper.mapToEntity(note)
        firestore
            .collection(DELETES_COLLECTION)
            .document(USER_ID)
            .collection(NOTES_COLLECTION)
            .document(entity.id)
            .set(entity)
            .await()
    }

    override suspend fun insertDeletedNotes(notes: List<Note>) {
        if (notes.size > 500) {
            throw Exception("Cannot insert more than 500 notes at a time into firestore.")
        }

        val collectionRef = firestore
            .collection(DELETES_COLLECTION)
            .document(USER_ID)
            .collection(NOTES_COLLECTION)

        firestore.runBatch { batch ->
            for (note in notes) {
                val entity = networkMapper.mapToEntity(note)
                val documentRef = collectionRef.document(note.id)
                batch.set(documentRef, entity)
            }
        }
    }

    override suspend fun deleteDeletedNote(note: Note) {
        firestore
            .collection(DELETES_COLLECTION)
            .document(USER_ID)
            .collection(NOTES_COLLECTION)
            .document(note.id)
            .delete()
            .await()
    }

    override suspend fun deleteAllNotes() {
        // Delete all notes from deletes node
        firestore
            .collection(DELETES_COLLECTION)
            .document(USER_ID)
            .delete()

        // Delete all notes from notes node
        firestore
            .collection(NOTES_COLLECTION)
            .document(USER_ID)
            .delete()
    }

    override suspend fun getDeletedNotes(): List<Note> {
        return networkMapper.entityListToNoteList(
            firestore
                .collection(DELETES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .get()
                .await()
                .toObjects(NoteNetworkEntity::class.java)
        )
    }

    override suspend fun searchNote(note: Note): Note? {
        return firestore
            .collection(NOTES_COLLECTION)
            .document(USER_ID)
            .collection(NOTES_COLLECTION)
            .document(note.id)
            .get()
            .await()
            .toObject(NoteNetworkEntity::class.java)?.let {
                networkMapper.mapFromEntity(it)
            }
    }

    override suspend fun getAllNotes(): List<Note> {
        return networkMapper.entityListToNoteList(
            firestore
                .collection(NOTES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .get()
                .await()
                .toObjects(NoteNetworkEntity::class.java)
        )
    }

    private fun getEntity(note: Note): NoteNetworkEntity {
        val entity = networkMapper.mapToEntity(note)
        entity.updated_at = Timestamp.now()
        return entity
    }

    companion object {
        const val NOTES_COLLECTION = "notes"
        const val USERS_COLLECTION = "users"
        const val DELETES_COLLECTION = "deletes"

        // TODO: hardcoded for single user because we don't have a login screen
        const val USER_ID = "NQcOb7ojPIRMWDuzheQXxqym2mb2"
        const val EMAIL = "jmcassis@gmail.com"
    }

}
