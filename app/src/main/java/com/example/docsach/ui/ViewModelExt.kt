package com.example.docsach.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.docsach.DocSachApp
import com.example.docsach.data.BookRepository

/** Pulls the app-wide repository out of the container without a DI framework. */
val CreationExtras.bookRepository: BookRepository
    get() {
        val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        return (app as DocSachApp).container.bookRepository
    }
