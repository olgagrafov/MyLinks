package com.olgag.wisavvy.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.olgag.wisavvy.model.URLS
import kotlinx.coroutines.*

class URLRepository(private val urlsDao: UrlsDao) {

    val allUrls: LiveData<List<URLS>> = urlsDao.getAllUrls()

    val allCategories: LiveData<List<URLS>> = urlsDao.getAllLiveCategories()

    val searchResults = MutableLiveData<List<URLS>>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun insertUrl(newUrl: URLS) {
        coroutineScope.launch(Dispatchers.IO) {
            urlsDao.insertUrl(newUrl)
        }
    }

    fun updateCategory(id: String, category: String) {
        coroutineScope.launch(Dispatchers.IO) {
            urlsDao.updateCategory(id, category)
        }
    }

    fun deleteUrl(id: String) {
        coroutineScope.launch(Dispatchers.IO) {
            urlsDao.deleteUrl(id)
        }
    }

//    fun findUrlsList(url: String) {
//        coroutineScope.launch(Dispatchers.Main) {
//            searchResults.value = asyncFind(url).await()
//        }
//    }

    fun findUrlsByCategories(category: String) {
        coroutineScope.launch(Dispatchers.Main) {
            searchResults.value = asyncFindUrlsByCategories(category).await()
        }
    }

    suspend fun deleteUrlByUrlString(url: String) : Int =
        coroutineScope.async(Dispatchers.IO) {
            urlsDao.deleteUrlByUrlString(url)
            return@async urlsDao.getCountOfRows()
        }.await()

//    private fun asyncFind(url: String): Deferred<List<URLS>?> =
//        coroutineScope.async(Dispatchers.IO) {
//            return@async urlsDao.findUrlsList(url)
//        }

    private fun asyncFindUrlsByCategories(category: String): Deferred<List<URLS>?> =
        coroutineScope.async(Dispatchers.IO) {
            return@async urlsDao.findUrlsByCategories(category)
        }


    fun asyncFindUrl(url: String): Deferred<Int> =
        coroutineScope.async(Dispatchers.IO) {
            return@async urlsDao.findUrl(url)
        }

//    fun asyncSise(): Deferred<Int> =
//        coroutineScope.async(Dispatchers.IO) {
//            return@async urlsDao.getCountOfRows()
//        }

    fun asyncCategories(): Deferred<List<URLS>?> =
        coroutineScope.async(Dispatchers.IO) {
            return@async urlsDao.getAllCategories()
        }

}