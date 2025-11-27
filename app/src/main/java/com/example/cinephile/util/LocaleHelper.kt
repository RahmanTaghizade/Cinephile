package com.example.cinephile.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "cinephile_prefs"
    private const val KEY_LANGUAGE = "language"
    
    private const val LANGUAGE_EN = "en"
    private const val LANGUAGE_FR = "fr"
    private const val LANGUAGE_RU = "ru"
    
    fun getSavedLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_EN) ?: LANGUAGE_EN
    }
    
    fun saveLanguage(context: Context, language: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
    
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun updateResources(context: Context, language: String): Resources {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config).resources
    }
    
    fun getFlagEmoji(language: String): String {
        return when (language) {
            LANGUAGE_EN -> "\uD83C\uDDEC\uD83C\uDDE7"
            LANGUAGE_FR -> "🇫🇷"
            LANGUAGE_RU -> "🇷🇺"
            else -> "\uD83C\uDDEC\uD83C\uDDE7"
        }
    }
    
    fun getLanguageDisplayName(language: String, context: Context): String {
        return when (language) {
            LANGUAGE_EN -> context.getString(com.example.cinephile.R.string.language_english)
            LANGUAGE_FR -> context.getString(com.example.cinephile.R.string.language_french)
            LANGUAGE_RU -> context.getString(com.example.cinephile.R.string.language_russian)
            else -> context.getString(com.example.cinephile.R.string.language_english)
        }
    }
}


