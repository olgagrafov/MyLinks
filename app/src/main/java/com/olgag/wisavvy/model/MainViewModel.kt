package com.olgag.wisavvy.model

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.olgag.wisavvy.db.URLRepository
import com.olgag.wisavvy.db.UrlRoomDatabase
import kotlinx.coroutines.Deferred

class MainViewModel(application: Application) : ViewModel() {

    val allURLs: LiveData<List<URLS>>
    val allCategories: LiveData<List<URLS>>
    private val repository: URLRepository
    val searchResults: MutableLiveData<List<URLS>>

    init {
        val urlDb = UrlRoomDatabase.getInstance(application)
        val urlDao = urlDb.urlDao()
        repository = URLRepository(urlDao)

        allURLs = repository.allUrls
        allCategories = repository.allCategories
        searchResults = repository.searchResults
    }


    fun findUrlByCategory(category: String) {
        repository.findUrlsByCategories(category)
    }

    fun updateCategory(urlId: String, category: String) {
        repository.updateCategory(urlId, category)
    }

    fun deleteURL(urlId: String) {
        repository.deleteUrl(urlId)
    }

    fun getAllCategories(): Deferred<List<URLS>?> {
        return repository.asyncCategories()
    }
}