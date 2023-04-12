package com.olgag.wisavvy.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "urls_list")
class URLS {
    @PrimaryKey(autoGenerate = false)
    @NonNull
    @ColumnInfo(name = "urlId")
    var id: String = UUID.randomUUID().toString()

    @ColumnInfo(name = "urlAddress")
    var urlAddress: String = ""

    @ColumnInfo(name = "urlServer")
    var urlServer: String = ""

    @ColumnInfo(name = "category")
    var category: String = ""

    constructor() {}
    constructor(urlAddress: String?) {
        this.id = id
        this.urlAddress = urlAddress.toString()
        this.urlServer = ""
        this.category = ""
    }

    constructor(urlAddress: String?, urlServer: String?, category: String?) {
        this.id = id
        this.urlAddress = urlAddress.toString()
        this.urlServer = urlServer.toString()
        this.category = category.toString()
    }

    override fun toString(): String {
        return category
    }
}
