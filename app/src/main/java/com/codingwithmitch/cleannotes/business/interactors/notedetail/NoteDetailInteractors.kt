package com.codingwithmitch.cleannotes.business.interactors.notedetail

import com.codingwithmitch.cleannotes.business.interactors.common.DeleteNote
import com.codingwithmitch.cleannotes.framework.presentation.notedetail.state.NoteDetailViewState

class NoteDetailInteractors(
    val deleteNote: DeleteNote<NoteDetailViewState>,
    val updateNote: UpdateNote
)
