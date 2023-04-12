package com.olgag.wisavvy.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.*
import com.olgag.wisavvy.db.URLRepository
import com.olgag.wisavvy.db.UrlRoomDatabase
import com.olgag.wisavvy.model.URLS
import com.olgag.wisavvy.ui.theme.WiSavvyTheme
import kotlinx.coroutines.runBlocking

const val ANIMATION_COUNT = 1
class Splash : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiSavvyTheme {
                Greeting(application)
            }
       }
    }
}

@SuppressLint("CoroutineCreationDuringComposition", "SuspiciousIndentation")
@Composable
fun Greeting(application: Application) {
    val activity = LocalContext.current as Activity
    val count = mySplash()
    val urlsCategories: List<URLS>  = checkSizeDB(application)
    val isNotEmpty = urlsCategories.isNotEmpty()
    //val urlsArray = if(isNotEmpty) arrayOf(urlsCategories) else arrayOfNulls<String>(urlsCategories.size)
    val urlsArray = arrayOfNulls<String>(urlsCategories.size)
    var i = 0
    urlsCategories.forEach() {
        urlsArray[i++] =  it.category
    }

    if(count > ANIMATION_COUNT) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.putExtra("dbIsNotEmpty", isNotEmpty)
        intent.putExtra("urlsCategories", urlsArray)
        activity.startActivity(intent)
        activity.finish()
    }
}

@Composable
fun mySplash() : Int {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("link.json"))
    val loadingAnimationState =
        animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )
    LottieAnimation(
        composition = composition,
        progress = { loadingAnimationState.progress },
        Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colors.onBackground,
                        MaterialTheme.colors.background
                    )
                )
            )
            .fillMaxSize()
    )
    return loadingAnimationState.iteration
}

fun checkSizeDB(application: Application): List<URLS> = runBlocking {
    val rep: URLRepository
    val urlDb = UrlRoomDatabase.getInstance(application)
    val urlDao = urlDb.urlDao()
    rep = URLRepository(urlDao)

    return@runBlocking rep.asyncCategories().await()!!
}
