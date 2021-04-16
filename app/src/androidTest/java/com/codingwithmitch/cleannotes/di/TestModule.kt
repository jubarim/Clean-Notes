package com.codingwithmitch.cleannotes.di

import androidx.room.Room
import com.codingwithmitch.cleannotes.business.domain.model.NoteFactory
import com.codingwithmitch.cleannotes.framework.datasource.cache.database.NoteDatabase
import com.codingwithmitch.cleannotes.framework.datasource.data.NoteDataFactory
import com.codingwithmitch.cleannotes.framework.presentation.TestBaseApplication
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object TestModule {
    @JvmStatic
    @Singleton
    @Provides
    fun provideNoteDb(app: TestBaseApplication): NoteDatabase {
        // db at runtime only, not persistent, for testing
        return Room.inMemoryDatabaseBuilder(
            app,
            NoteDatabase::class.java
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideFirestoreSettings(): FirebaseFirestoreSettings {
        return FirebaseFirestoreSettings.Builder()
            .setHost("10.0.2.2:8080")
            .setSslEnabled(false)
            .setPersistenceEnabled(false)
            .build()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideFirebaseFirestore(settings: FirebaseFirestoreSettings): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        //firestore.useEmulator("10.0.2.2", 8080);
        firestore.firestoreSettings = settings
        return firestore
    }


    @JvmStatic
    @Singleton
    @Provides
    fun provideNoteDataFactory(
        application: TestBaseApplication,
        noteFactory: NoteFactory
    ): NoteDataFactory {
        return NoteDataFactory(application, noteFactory)
    }
}
