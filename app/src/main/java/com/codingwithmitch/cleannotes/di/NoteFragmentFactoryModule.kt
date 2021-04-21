package com.codingwithmitch.cleannotes.di

import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import com.codingwithmitch.cleannotes.business.domain.util.DateUtil
import com.codingwithmitch.cleannotes.framework.presentation.common.NoteFragmentFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object NoteFragmentFactoryModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideNoteFragmentFactory(
        viewModelFactory: ViewModelProvider.Factory,
        dateUtil: DateUtil
    ): FragmentFactory {
        return NoteFragmentFactory(
            viewModelFactory,
            dateUtil
        )
    }
}
