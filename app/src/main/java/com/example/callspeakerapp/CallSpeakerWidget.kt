package com.example.callspeakerapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CallSpeakerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val startIntent = Intent(context, CallSpeakerService::class.java).apply {
                action = "START_SERVICE"
            }
            val startPendingIntent = PendingIntent.getService(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.button_start_service, startPendingIntent)

            val stopIntent = Intent(context, CallSpeakerService::class.java).apply {
                action = "STOP_SERVICE"
            }
            val stopPendingIntent = PendingIntent.getService(context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.button_stop_service, stopPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
