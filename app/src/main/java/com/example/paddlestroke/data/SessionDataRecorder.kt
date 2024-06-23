package com.example.paddlestroke.data

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


class SessionDataRecorder {

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

        val sb = StringBuffer()
        sb.append(System.currentTimeMillis()).append(" ")
            .append(dataRecord)
        bufferedWriter?.write(sb.toString())
        bufferedWriter?.newLine()
    }

}