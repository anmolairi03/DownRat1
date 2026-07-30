package com.example.cycletracker.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CycleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CycleWidget()
}
