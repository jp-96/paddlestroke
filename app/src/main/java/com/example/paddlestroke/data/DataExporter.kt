package com.example.paddlestroke.data

interface DataExporter {
    val columnNames: Array<String>
    fun exportData(data: Any?): Array<Any>?
}
