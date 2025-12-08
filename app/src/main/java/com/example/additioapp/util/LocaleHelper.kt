package com.example.additioapp.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    
    private const val PREF_LANGUAGE = "app_language"
    private const val PREFS_NAME = "additio_prefs"
    
    fun setLocale(context: Context, languageCode: String): Context {
        persist(context, languageCode)
        return updateResources(context, languageCode)
    }
    
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_LANGUAGE, "") ?: ""
    }
    
    fun onAttach(context: Context): Context {
        val lang = getLanguage(context)
        return if (lang.isEmpty()) {
            context
        } else {
            setLocale(context, lang)
        }
    }
    
    private fun persist(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_LANGUAGE, languageCode).apply()
    }
    
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "fr" -> "Français"
            "ar" -> "العربية"
            else -> "System Default"
        }
    }
    
    fun getSupportedLanguages(): Array<String> {
        return arrayOf("", "en", "fr", "ar")
    }
    
    fun getSupportedLanguageNames(): Array<String> {
        return arrayOf("System Default", "English", "Français", "العربية")
    }
}
