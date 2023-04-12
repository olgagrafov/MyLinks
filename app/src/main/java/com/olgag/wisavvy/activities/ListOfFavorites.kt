package com.olgag.wisavvy.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.LAYOUT_DIRECTION_LTR
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
//import com.google.accompanist.web.WebView
//import com.google.accompanist.web.rememberWebViewState

import com.olgag.wisavvy.R
import com.olgag.wisavvy.ui.theme.WiSavvyTheme
import com.olgag.wisavvy.model.MainViewModel
import com.olgag.wisavvy.model.URLS
import com.olgag.wisavvy.service.MainViewModelFactory
import kotlinx.coroutines.runBlocking
import me.saket.cascade.CascadeDropdownMenu

lateinit var viewModel: MainViewModel
class ListOfFavorites : ComponentActivity() {
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WiSavvyTheme {
                val owner = LocalViewModelStoreOwner.current
                owner?.let {
                     viewModel = viewModel(
                        it,
                        "MainViewModel",
                        MainViewModelFactory(
                            LocalContext.current.applicationContext
                                    as Application
                        )
                    )
                    val allCategories by viewModel.allCategories.observeAsState(listOf())
                    val allURLs by viewModel.allURLs.observeAsState(listOf())
                    val searchResults by viewModel.searchResults.observeAsState(listOf())

                    var searching by remember { mutableStateOf(false) }

                    val list = if (searching) searchResults else allURLs

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { AppBarMenu(
                                    allCategories = allCategories,
                                    setSearching =  { newSearching ->
                                        searching = newSearching  },
                                )},
                                backgroundColor = MaterialTheme.colors.background
                            )
                        },
                        content = {
                            Box(
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
                            ) {
                                MainScreen(
                                    list = list,
                                    searching =  searching,
                                    allCategories = allCategories,
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
       super.onBackPressed()
        goToMain(this, "")
    }
}

@Composable
fun MainScreen(
    list: List<URLS>,
    searching: Boolean,
    allCategories: List<URLS>
) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .padding(10.dp),
    ) {
        items(list) { item ->
            WebViewCard(item = item ,allCategories = allCategories,urlAddress = item.urlAddress, urlServer =  item.urlServer,
                deleteRow = { viewModel.deleteURL(item.id)
                    if(searching) viewModel.findUrlByCategory(item.category)
            },
                changeCategories = { if(searching) viewModel.findUrlByCategory(item.category)
                },
            )
        }
    }
}


@Composable
fun AppBarMenu(
    allCategories: List<URLS>,
    setSearching: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val config  = LocalConfiguration.current
    val allCollections: String = context.resources.getString(R.string.all_collections)
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(allCollections) }

    IconButton(
        onClick = { goToMain(context as Activity, "")},
    )
    {
        if(config.layoutDirection == LAYOUT_DIRECTION_LTR)
            Icon(Icons.Rounded.ArrowBack, null)
        else
            Icon(Icons.Rounded.ArrowForward, null)
    }

    Text(text = context.resources.getString(R.string.collection), fontSize = 20.sp,  modifier = Modifier.padding(end = 10.dp))
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopEnd)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedText,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.clickable(onClick = { expanded = !expanded })
            )
            IconButton(
                onClick = { expanded = !expanded },
            )
            {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, Modifier.size(26.dp), tint = MaterialTheme.colors.onBackground
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allCategories.forEach() {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = it.category,
                                color = MaterialTheme.colors.background,
                                fontSize = 20.sp
                            )
                        },
                        onClick = {
                            selectedText = it.category
                            expanded = false
                            setSearching(true)
                            viewModel.findUrlByCategory(it.category)
                        },
                    )
                }
            DropdownMenuItem(
                text = {
                    Text(
                        text = allCollections,
                        color = MaterialTheme.colors.background,
                        fontSize = 20.sp
                    )
                },
                onClick = {
                    selectedText = allCollections
                    expanded = false
                    setSearching(false)
                },
            )
        }

    }
}

//@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewCard(
    urlAddress: String,
    urlServer: String,
    deleteRow: () -> Unit,
    allCategories: List<URLS>,
    changeCategories: () -> Unit,
    item: URLS,
){
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    //val state = rememberWebViewState(urlAddress)

    Box(modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        .background(color = Color.White, shape = RoundedCornerShape(size = 12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = urlServer, color = MaterialTheme.colors.background, fontSize = 18.sp )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = urlServer.plus("favicon.ico"),
                    contentDescription = null,
                    modifier = Modifier.width(36.dp).height(36.dp),
                    error = painterResource(R.drawable.link)
                )
                Text(text = urlAddress,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
//        WebView(
//            state,
//            onCreated = { it.settings.javaScriptEnabled = true },
//            captureBackPresses = false,
//        )
//        if (state.isLoading) {
//            CircularProgressIndicator(Modifier.align(alignment = Alignment.Center))
//        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopEnd)
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .align(alignment = Alignment.TopEnd),
                onClick = { expanded = !expanded },
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null,
                    tint = MaterialTheme.colors.onBackground
                )
            }

            CascadeDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                 DropItem(context.resources.getString(R.string.more),
                     Icons.Rounded.More) { goToMain(context as Activity, urlAddress) }
                 DropItem(context.resources.getString(R.string.share),
                     Icons.Rounded.Share) { expanded = false
                     share(context, urlAddress) }
                 DropItem(context.resources.getString(R.string.delete),
                     Icons.Rounded.Delete) { expanded = false
                     deleteRow() }
                Divider()
                DropdownMenuItem(
                    text = { Text(
                        color = MaterialTheme.colors.background,
                        text = context.resources.getString(R.string.change_category)) },
                    children = {
                        allCategories.forEach() {
                            if (item.category != it.category) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            color = MaterialTheme.colors.background,
                                            text = it.category
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateCategory(item.id, it.category)
                                        expanded = false
                                        changeCategories()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DropItem(txt: String, icon: ImageVector, callBack: () -> Unit) {
    DropdownMenuItem(
        text = { Text(
            color = MaterialTheme.colors.background,
            text = txt)},
        onClick = callBack,
        trailingIcon  = {
            Icon(
                imageVector = icon,
                null,
                tint =  MaterialTheme.colors.background
            )
        },
    )
}

 fun goToMain(activity: Activity, urlName: String) {
     val categories: List<URLS> = getCategories()
     val urlsArray = arrayOfNulls<String>(categories.size)
     for(i in urlsArray.indices){
         urlsArray[i] = categories[i].toString()
     }

    val intent = Intent(activity,MainActivity::class.java)
    intent.putExtra("dbIsNotEmpty", categories.isNotEmpty())
    if(urlName.isNotEmpty()) intent.putExtra("qrUrl", urlName)
    intent.putExtra("urlsCategories", urlsArray)
    activity.startActivity(intent)
    activity.finish()
}

fun getCategories(): List<URLS> = runBlocking{
    return@runBlocking viewModel.getAllCategories().await()!!
}

fun share(context: Context, qrUrl: String) {
    val intent = Intent()
    intent.action = Intent.ACTION_SEND
    intent.putExtra(Intent.EXTRA_TEXT, qrUrl)
    intent.type = "text/plain"
    context.startActivity(Intent.createChooser(intent, "Share To:"))
}
