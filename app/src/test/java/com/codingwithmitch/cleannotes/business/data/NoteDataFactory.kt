package com.codingwithmitch.cleannotes.business.data

import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NoteDataFactory(
    private val testClassLoader: ClassLoader
) {

    fun produceListOfNotes(): List<Note> {
        val notes: List<Note> = Gson()
            .fromJson(
                getNotesFromFile("note_list.json"),
                object : TypeToken<List<Note>>(){}.type
            )

        return notes
    }

    fun produceHashMapOfNotes(notes: List<Note>): HashMap<String, Note> {
        val map = HashMap<String, Note>()

        notes.forEach {
            map[it.id] = it
        }

        return map
    }

    fun produceEmptyListOfNotes(): List<Note> = emptyList()

    private fun getNotesFromFile(fileName: String): String {
        return testClassLoader.getResource(fileName).readText()
    }
}