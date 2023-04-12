package com.olgag.wisavvy.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.olgag.wisavvy.R
import java.text.SimpleDateFormat
import java.util.*


class Helper(private val context: Context) {

    @SuppressLint("SimpleDateFormat")
    fun initGreeting(): String{
        val arrGreeting = context.resources.getStringArray(R.array.greeting)
        val simpleHourFormat = SimpleDateFormat("HH").format(Date()).toInt()
       // Log.i("simpleHourFormat: ", simpleHourFormat.toString())
        val greeting =
            when (simpleHourFormat) {
                in 12..17 -> {
                    arrGreeting[1]
                }
                in 18..22 -> {
                    arrGreeting[2]
                }
                in 23 .. 24 -> {
                    arrGreeting[3]
                }
                in 0 .. 4 -> {
                    arrGreeting[3]
                }
                else -> arrGreeting[0]
            }
        return greeting
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    fun toastMassage(strMassage: String){
        Toast.makeText(context, strMassage, Toast.LENGTH_SHORT).show()
    }

}