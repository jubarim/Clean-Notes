package com.codingwithmitch.cleannotes.framework.presentation.notedetail.state

import android.os.Parcelable
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.state.ViewState
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NoteDetailViewState(

    var note: Note? = null,

    // Prevents unnecessary savings when there are no changes and user
    // changes the app (onPause is called, where it would update the note)
    var isUpdatePending: Boolean? = null

) : Parcelable, ViewState
