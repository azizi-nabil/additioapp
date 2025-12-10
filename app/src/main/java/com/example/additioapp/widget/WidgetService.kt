package com.example.additioapp.widget

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetDataProvider(applicationContext, intent)
    }
}
