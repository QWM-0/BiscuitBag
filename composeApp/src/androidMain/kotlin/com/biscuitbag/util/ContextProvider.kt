package com.biscuitbag.util

import android.content.Context

object ContextProvider {
    lateinit var appContext: Context

    val isInitialized: Boolean get() = ::appContext.isInitialized
}
