package com.codegalaxy.barcodescanner.service

import android.content.Context
import android.content.SharedPreferences

class DeviceStoreService(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("DeviceStore", Context.MODE_PRIVATE)

    fun saveData(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun loadData(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun removeData(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
}
