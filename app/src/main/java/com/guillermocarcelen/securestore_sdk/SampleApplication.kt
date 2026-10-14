package com.guillermocarcelen.securestore_sdk

import android.app.Application
import com.guillermocarcelen.securestore.SecureStore

class SampleApplication : Application() {
    val store by lazy {
        SecureStore.init(
            this
        )
    }
}