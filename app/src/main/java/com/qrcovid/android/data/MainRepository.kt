package com.qrcovid.android.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainRepository {
    private lateinit var client: OkHttpClient
    private lateinit var retrofit: Retrofit
    private lateinit var server: QrStorageServer

    init {
        initClient()
        initRetrofit()
        initServer()
    }

    private fun initClient() {
        val interceptor = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    private fun initRetrofit() {
        retrofit = Retrofit.Builder()
            .baseUrl(QR_STORAGE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun initServer() {
        server = retrofit.create(QrStorageServer::class.java)
    }

    companion object {
        private val QR_STORAGE_URL = "https://domain.name/api/v2/"
    }
}