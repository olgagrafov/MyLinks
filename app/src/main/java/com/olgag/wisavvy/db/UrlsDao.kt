package com.olgag.wisavvy.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.olgag.wisavvy.model.URLS

@Dao
interface UrlsDao {
    @Insert
    fun insertUrl(urls: URLS)

    @Query("UPDATE urls_list SET category = :category WHERE urlId = :urlId")
    fun updateCategory(urlId: String, category: String)

    @Query("SELECT COUNT(*) FROM urls_list WHERE urlAddress = :urlAddress")
    fun findUrl(urlAddress: String): Int

    @Query("SELECT * FROM urls_list WHERE category = :category")
    fun findUrlsByCategories(category: String): List<URLS>

    @Query("SELECT * FROM urls_list WHERE instr (urlAddress , :urlAddress) > 0")
    fun findUrlsList(urlAddress: String): List<URLS>

    @Query("DELETE FROM urls_list WHERE urlId = :urlId")
    fun deleteUrl(urlId: String)

    @Query("DELETE FROM urls_list WHERE urlAddress = :urlAddress")
    fun deleteUrlByUrlString(urlAddress: String)

    @Query("SELECT * FROM urls_list")
    fun getAllUrls(): LiveData<List<URLS>>

    @Query("SELECT * , COUNT(*) AS count FROM urls_list GROUP BY category")
    fun getAllLiveCategories(): LiveData<List<URLS>>

    @Query("SELECT * , COUNT(*) AS count FROM urls_list GROUP BY category")
    fun getAllCategories(): List<URLS>

    @Query("SELECT COUNT(*) FROM urls_list")
    fun getCountOfRows(): Int
}