package com.example.qrscanner

import android.app.Application
import android.os.Bundle
import com.google.zxing.client.result.ParsedResult

class QRApp : Application() {
    var lastParsedResult: ParsedResult? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: QRApp
            private set
    }
}