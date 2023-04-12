package com.olgag.wisavvy.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Patterns
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.olgag.wisavvy.R
import com.olgag.wisavvy.data.Helper
import com.olgag.wisavvy.db.URLRepository
import com.olgag.wisavvy.db.UrlRoomDatabase
import com.olgag.wisavvy.model.URLS
import com.olgag.wisavvy.ui.theme.WiSavvyTheme
import kotlinx.coroutines.runBlocking
import java.util.*

@SuppressLint("StaticFieldLeak")
private lateinit var webView: WebView
@SuppressLint("StaticFieldLeak")
private lateinit var helper: Helper

private var qrUrl: String? = ""
private var rep: URLRepository? = null
private var dbIsNotEmpty: Boolean? = true
private lateinit var categories: MutableSet<String>

class MainActivity : ComponentActivity() {

    private val barcodeLauncher: ActivityResultLauncher<ScanOptions> = registerForActivityResult(
        ScanContract()
    ) {
            result ->
        if (result.contents == null) {
            helper.toastMassage(resources.getString(R.string.scanning_cancelled))
        } else {
            qrUrl = if  (Patterns.WEB_URL.matcher(result.contents).matches()) result.contents else null
        }
        qrUrl?.let { it -> webView.loadUrl(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlDb = UrlRoomDatabase.getInstance(application)
        val urlDao = urlDb.urlDao()
        rep = URLRepository(urlDao)
    }


    override fun onResume() {
        super.onResume()

        qrUrl = intent.getStringExtra("qrUrl")
        dbIsNotEmpty = intent.getBooleanExtra("dbIsNotEmpty", false)
        categories = (intent.getStringArrayExtra("urlsCategories")!!.toSet()).toMutableSet()

        setContent {
            WiSavvyTheme {
                helper = Helper(this)
                val strGreeting = helper.initGreeting()
                App(strGreeting, barcodeLauncher)
          }
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun App(
    greeting: String,
    barcodeLauncher: ActivityResultLauncher<ScanOptions>,
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(dbIsNotEmpty) }
    var urlEmpty by remember { mutableStateOf(true) }

    Scaffold(
        topBar =  { TopAppBar(title = {
            ExitFromApp(context as Activity)
            Text(greeting, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxSize()) {
                    FindOnGoogle(context)
                    ScanQRCode(context, barcodeLauncher)
                    if(!urlEmpty) {
                        ShareUrl(context)
                    }
                    GotoFavorite(context, isEnabled)
            } },
            backgroundColor = MaterialTheme.colors.background)},
       content = {
           MainContent( barcodeLauncher, context,
               setIsEnabled = { isEnabled = (dbIsNotEmpty == true) },
               setUrl =  { urlEmpty = false })
       }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainContent(
    barcodeLauncher: ActivityResultLauncher<ScanOptions>,
    context: Context,
    setIsEnabled: () -> Unit,
    setUrl: () -> Unit
) {
    var backEnabled by remember { mutableStateOf(false) }
    var tmpUrl by remember { mutableStateOf("") }
    var isAddToFavorite  by remember { mutableStateOf(true) }

    AndroidView(
        factory = {
        WebView(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    backEnabled = view.canGoBack()
                    qrUrl = url
                    tmpUrl = url.toString()
                    isAddToFavorite = !checkIfUrlExistIntoDBBeforeAddToFavorite()
                }
            }
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            qrUrl?.let { it1 -> loadUrl(it1) }
            webView = this
        }
    }, update =  {
        qrUrl?.let { it1 -> it.loadUrl(it1) }
        webView = it
    })

    if (qrUrl == null && tmpUrl.isEmpty()) {
        ScanFloatingButtonDemo(context, barcodeLauncher)
    }
    else {
        setUrl()
        if (isAddToFavorite) {
            AddToFavorite(setIsEnabled, setAddToFavorite = { isAddToFavorite = false }, context)
        } else {
            RemoveFromFavorite(setIsEnabled, setAddToFavorite = { isAddToFavorite = true })
        }
    }

    BackHandler(enabled = backEnabled) {
        webView.goBack()
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun GotoFavorite(context: Context, isEnabled: Boolean?) {
    val activity = context as Activity
        IconButton(
            onClick = { activity.startActivity(Intent(activity, ListOfFavorites::class.java))
                      activity.finish()},
            enabled = isEnabled == true
        )
        {
            Icon(Icons.Rounded.Bookmarks, null)
        }
}

@Composable
fun FindOnGoogle(context: Activity) {
    IconButton(
        onClick = { if (helper.isInternetAvailable(context))  webView.loadUrl("https://www.google.com")
            else helper.toastMassage(context.resources.getString(R.string.no_internet)) }) {
        Icon( painter = painterResource(id = R.drawable.igoogle), null, tint = Color.Unspecified)
    }
}

@Composable
fun ScanQRCode(context: Context, barcodeLauncher: ActivityResultLauncher<ScanOptions>) {
    IconButton(
        onClick = { scan(context, barcodeLauncher) }) {
        Icon( Icons.Rounded.QrCodeScanner, null)
    }
}

@Composable
fun ShareUrl(context: Context) {
    IconButton(
        onClick = { share(context) }) {
        Icon( Icons.Rounded.Share, null, tint = MaterialTheme.colors.onBackground)
    }
}

@Composable
fun ExitFromApp(activity: Activity) {
    val openAlert = remember { mutableStateOf(false) }

    IconButton(
        onClick = { openAlert.value = true }) {
        Icon(Icons.Rounded.HighlightOff, null, tint = MaterialTheme.colors.onBackground)
    }

    if (openAlert.value) {
        AlertDialog(
            onDismissRequest = { openAlert.value = false },
            title = {
                Text(text = activity.resources.getString(R.string.do_you_want_exit), fontSize = 20.sp, fontWeight = FontWeight.Bold)},
            dismissButton = {
                Button(
                    onClick = { openAlert.value = false },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                ) {
                    Text(color = MaterialTheme.colors.onBackground , text = activity.resources.getString(R.string.dont_exit))
                }},
            confirmButton = {
                Button(
                    onClick = { activity.finish() },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                ) {
                    Text(color = MaterialTheme.colors.onBackground , text = activity.resources.getString(R.string.exit))
                }}
        )
    }
}

@Composable
fun ScanFloatingButtonDemo(
    context: Context,
    barcodeLauncher: ActivityResultLauncher<ScanOptions>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(alignment = Alignment.Center),
            onClick = { scan(context, barcodeLauncher) },
            text = { Text(color = MaterialTheme.colors.onBackground , text = context.resources.getString(R.string.scan_or_code)) },
            icon = { Icon(Icons.Rounded.QrCodeScanner, null, tint = MaterialTheme.colors.onBackground) })
    }
}

@Composable
fun RemoveFromFavorite(setIsEnabled: () -> Unit, setAddToFavorite: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            modifier = Modifier
                .padding(all = 8.dp)
                .align(alignment = Alignment.BottomEnd),
            onClick = {
                setAddToFavorite()
                removeFromFavorite(setIsEnabled)
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.BookmarkRemove,
                null,
                Modifier.size(36.dp),
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Composable
fun AddToFavorite(setIsEnabled: () -> Unit, setAddToFavorite: () -> Unit, context: Context) {
    val openDialog = remember { mutableStateOf(false)}
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            modifier = Modifier
                .padding(all = 8.dp)
                .align(alignment = Alignment.BottomEnd),
           onClick = { openDialog.value = true},
        ) {
            Icon(
                imageVector = Icons.Rounded.BookmarkAdd,
                null,
                Modifier.size(36.dp),
                tint = MaterialTheme.colors.onBackground
            )
        }
    }

    if (openDialog.value) {
        ShowCategory(context, openDialog,  setIsEnabled, setAddToFavorite )
    }

}

@Composable
fun ShowCategory(context: Context, openDialog: MutableState<Boolean>, setIsEnabled: () -> Unit, setAddToFavorite: () -> Unit) {
    var selectedItem by remember { mutableStateOf("") }
    var collectionNewName by remember { mutableStateOf("") }
    var isCreteNewCollection by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {openDialog.value = false},
        content = {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp, top = 20.dp)){
            Surface(shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxHeight()) {
                 Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = { openDialog.value = false }) {
                        Icon(
                            Icons.Rounded.Cancel,
                            null,
                            tint = Color.Black
                        )
                    }
                }
                Column( modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = context.resources.getString(R.string.select_collection) ,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(500.dp)
                            .padding(bottom = 80.dp),
                        content = {
                            val iterator = categories.iterator()
                            iterator.forEach {
                                if(it.isNotBlank()) {
                                    item {
                                        Row(
                                            Modifier.selectable(
                                                selected = (selectedItem == it),
                                                onClick = {
                                                    selectedItem = it
                                                    isCreteNewCollection = false
                                                    isError = false
                                                }
                                            ), verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (selectedItem == it),
                                                onClick = {
                                                    selectedItem = it
                                                    isCreteNewCollection = false
                                                    isError = false
                                                }
                                            )
                                            Text(text = it, style = MaterialTheme.typography.h6)
                                        }
                                    }
                                }
                            }
                            item {
                                Row(
                                    Modifier.selectable(
                                        selected = isCreteNewCollection,
                                        onClick = { selectedItem = " "
                                            isCreteNewCollection = true }
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isCreteNewCollection,
                                        onClick = { selectedItem = " "
                                            isCreteNewCollection = true }
                                    )
                                    Text(
                                        text = context.resources.getString(R.string.create_new_collection),
                                        style = MaterialTheme.typography.h6
                                    )
                                }
                                Row(
                                    Modifier
                                        .padding(start = 12.dp)
                                ) {
                                    AnimatedVisibility(
                                        visible = isCreteNewCollection,
                                        enter = fadeIn(animationSpec = tween(2000)),
                                        exit = fadeOut(animationSpec = tween(2000))
                                    ) {
                                        OutlinedTextField(
                                            value = collectionNewName,
                                            onValueChange = {
                                                if (it.trim().length <= 15) collectionNewName =
                                                    it.trim()
                                                selectedItem = collectionNewName
                                                isError = false
                                            },
                                            label = { Text(text = context.resources.getString(R.string.new_collection)) },
                                            placeholder = {
                                                Text(
                                                    text = context.resources.getString(
                                                        R.string.new_collection
                                                    )
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { collectionNewName = "" }) {
                                                    if (isError) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Error,
                                                            null,
                                                            tint = Color.Red
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Clear,
                                                            null
                                                        )
                                                    }
                                                }
                                            },
                                            isError = isError
                                        )
                                    }
                                }
                                if (isError) {
                                    Text(
                                        text = context.resources.getString(R.string.new_collection_empty),
                                        color = Color.Red,
                                        style = MaterialTheme.typography.caption,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
                   Box(modifier = Modifier
                       .fillMaxSize()
                       .padding(bottom = 20.dp)) {
                       Button(
                           modifier = Modifier
                               .align(alignment = Alignment.BottomCenter),
                           onClick = { isError = (isCreteNewCollection && collectionNewName.isBlank())
                               if(!isError) {
                                   if(selectedItem.isEmpty()) {
                                       addToFavorite(context.resources.getString(R.string.uncategorized))
                                   }
                                   else {
                                       addToFavorite(selectedItem)
                                       categories.add(selectedItem)
                                   }
                                   setIsEnabled()
                                   setAddToFavorite()
                                   openDialog.value = false
                               } },
                           shape = RoundedCornerShape(28.dp),
                           colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                       ) {
                           Icon(Icons.Rounded.Save, null, tint = Color.White)
                           Text(
                               modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                               text = if (selectedItem.isEmpty()) context.resources.getString(R.string.save_uncategorized) else context.resources.getString(R.string.save),
                               color = Color.White
                           )
                       }
                   }
               }
            }
        },
    )
}

fun addToFavorite(categoryName: String)  {
    val chars: CharArray = webView.url.toString().toCharArray()
    var count = 0
    val urlServer = buildString {
        for(c in chars){
            append(c)
            if(c == '/')
                count++
            if(count == 3)
                break
        }
    }
    rep!!.insertUrl(URLS(webView.url, urlServer, categoryName))
    dbIsNotEmpty = true
}

fun removeFromFavorite(setIsEnabled: () -> Unit)  = runBlocking {
    dbIsNotEmpty  =  rep!!.deleteUrlByUrlString(webView.url.toString()) > 0
    if(dbIsNotEmpty  == false) {
        setIsEnabled()
    }
}

fun checkIfUrlExistIntoDBBeforeAddToFavorite(): Boolean = runBlocking {
    return@runBlocking rep!!.asyncFindUrl(webView.url.toString()).await() > 0
}

fun scan(context: Context, barcodeLauncher: ActivityResultLauncher<ScanOptions>) {
    if (helper.isInternetAvailable(context)) {
        val options = ScanOptions()
        //options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES) for BarCode
        options.setPrompt(context.resources.getString(R.string.scan_qr))
        options.setBeepEnabled(false)
        options.setOrientationLocked(true)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }
    else {
        helper.toastMassage(context.resources.getString(R.string.no_internet))
    }
}

fun share(context: Context) {
    val intent= Intent()
    intent.action=Intent.ACTION_SEND
    intent.putExtra(Intent.EXTRA_TEXT,qrUrl)
    intent.type="text/plain"
    context.startActivity(Intent.createChooser(intent,"Share To:"))
}