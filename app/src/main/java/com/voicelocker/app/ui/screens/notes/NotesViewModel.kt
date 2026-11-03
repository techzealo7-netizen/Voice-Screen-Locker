package com.voicelocker.app.ui.screens.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicelocker.app.data.model.Note
import com.voicelocker.app.data.repository.NoteRepository
import com.voicelocker.app.data.repository.StoreNoteRepository
import com.voicelocker.app.domain.service.AiService
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NoteRepository = StoreNoteRepository()
) : ViewModel() {

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    var isBusy by mutableStateOf(false)
        private set

    init {
        loadNotes()
    }

    fun loadNotes() {
        notes = repository.getNotes()
    }

    fun deleteNote(id: String) {
        repository.deleteNote(id)
        loadNotes()
    }

    fun saveNote(id: String?, title: String, text: String) {
        if (id != null) {
            val existing = repository.getNoteById(id)
            if (existing != null) {
                existing.text = text
                if (title.isNotBlank()) existing.title = title
                repository.updateNote(existing)
            }
        } else {
            val newNote = Note(
                id = System.currentTimeMillis().toString(),
                title = title.ifBlank { "New Note" },
                text = text
            )
            repository.addNote(newNote)
        }
        loadNotes()
    }

    fun summarizeNote(id: String, onDone: () -> Unit = {}) {
        val note = repository.getNoteById(id) ?: return
        isBusy = true
        viewModelScope.launch {
            try {
                val summary = AiService.summarize(note.text)
                note.summary = summary
                repository.updateNote(note)
                loadNotes()
            } finally {
                isBusy = false
                onDone()
            }
        }
    }
}

