package com.example.paddlestroke.data

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


class SessionDataRecorder {

    companion object{
        private const val END_OF_RECORD: String = "@@"
    }

    private var bufferedWriter: BufferedWriter? = null

    fun open(file: File) {
        close()
        bufferedWriter = BufferedWriter(FileWriter(file))
    }

    fun close() {
        bufferedWriter?.flush()
        bufferedWriter?.close()
        bufferedWriter = null
    }

    fun log(dataRecord: DataRecord) {
        if (bufferedWriter==null) return

        if (dataRecord.type == DataRecord.Type.LOCATION) {
            val s = dataRecord.dataToString()
        }
        val sb = StringBuffer()
        sb.append(System.currentTimeMillis()).append(" ")
            .append(dataRecord).append(END_OF_RECORD)
        bufferedWriter?.write(sb.toString())
        bufferedWriter?.newLine()
    }

}