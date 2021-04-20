package com.codingwithmitch.cleannotes.business.data.cache.implementation

import com.codingwithmitch.cleannotes.business.data.cache.abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.framework.datasource.cache.abstraction.NoteDaoService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteCacheDataSourceImpl
@Inject
constructor(
    private val noteDaoService: NoteDaoService
): NoteCacheDataSource {
    override suspend fun insertNote(note: Note) =
        noteDaoService.insertNote(note)

    override suspend fun deleteNote(primaryKey: String) =
        noteDaoService.deleteNote(primaryKey)

    override suspend fun deleteNotes(notes: List<Note>) =
        noteDaoService.deleteNotes(notes)

    override suspend fun updateNote(
        primaryKey: String,
        newTitle: String,
        newBody: String,
        timestamp: String?
    ) = noteDaoService.updateNote(primaryKey, newTitle, newBody, timestamp)

    override suspend fun getAllNotes(): List<Note> =
        noteDaoService.getAllNotes()

    override suspend fun searchNotes(query: String, filterAndOrder: String, page: Int): List<Note> {
        return noteDaoService.returnOrderedQuery(
            query, filterAndOrder, page
        )
    }

    override suspend fun searchNoteById(primaryKey: String) =
        noteDaoService.searchNoteById(primaryKey)

    override suspend fun getNumNotes() =
        noteDaoService.getNumNotes()

    override suspend fun insertNotes(notes: List<Note>) =
        noteDaoService.insertNotes(notes)

}