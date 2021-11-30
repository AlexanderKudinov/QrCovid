package com.qrcovid.android.data

import retrofit2.http.POST

interface QrStorageServer {

    @POST("order/get-depart-token")
    suspend fun getQrToken(): String
}