package com.voicelocker.app.data.repository

import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.Note

interface NoteRepository {
    fun getNotes(): List<Note>
    fun getNoteById(id: String): Note?
    fun saveNotes(notes: List<Note>)
    fun addNote(note: Note)
    fun updateNote(note: Note)
    fun deleteNote(id: String)
}

class StoreNoteRepository : NoteRepository {
    override fun getNotes(): List<Note> = Store.notes

    override fun getNoteById(id: String): Note? = Store.notes.firstOrNull { it.id == id }

    override fun saveNotes(notes: List<Note>) = Store.saveNotes(notes)

    override fun addNote(note: Note) {
        val list = Store.notes.toMutableList()
        list.add(0, note)
        Store.saveNotes(list)
    }

    override fun updateNote(note: Note) {
        val list = Store.notes.toMutableList()
        val index = list.indexOfFirst { it.id == note.id }
        if (index != -1) {
            list[index] = note
            Store.saveNotes(list)
        }
    }

    override fun deleteNote(id: String) {
        Store.saveNotes(Store.notes.filter { it.id != id })
    }
}

