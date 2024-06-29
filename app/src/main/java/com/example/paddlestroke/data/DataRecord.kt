package com.example.paddlestroke.data

import com.example.paddlestroke.data.DataRecordSerializer.BOOLEAN
import com.example.paddlestroke.data.DataRecordSerializer.DISTANCE
import com.example.paddlestroke.data.DataRecordSerializer.DOUBLE
import com.example.paddlestroke.data.DataRecordSerializer.DOUBLE_ARR
import com.example.paddlestroke.data.DataRecordSerializer.FLOAT
import com.example.paddlestroke.data.DataRecordSerializer.FLOAT_ARR
import com.example.paddlestroke.data.DataRecordSerializer.INT
import com.example.paddlestroke.data.DataRecordSerializer.LONG
import com.example.paddlestroke.data.DataRecordSerializer.PARAMETER
import java.util.Locale

class DataRecord(val type: Type, val timestamp: Long, val data: Any?) {
    enum class Type(
        val isReplayableEvent: Boolean = false,
        val isBusEvent: Boolean = true,
        val dataExporter: DataExporter? = null,
        val dataParser: DataRecordSerializer? = null
    ) {
        UUID,
        RECORDING_START,
        RECORDING_COUNTDOWN(false, object : DataRecordSerializer() {
            override fun doSerialize(data: Any?): String {
                val vals = data as Array<*>?
                /* tag, countdown */
                return String.format(Locale.US, "%s,%d", *vals!!)
            }

            public override fun doParse(s: String): Any? {
                val tokens = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                /* tag, countdown */
                return arrayOf<Any>(tokens[0], tokens[1])
            }
        }),
        STROKE_DROP_BELOW_ZERO,
        STROKE_RISE_ABOVE_ZERO,
        STROKE_POWER_START,
        ROWING_STOP(false, object : DataRecordSerializer() {
            override fun doSerialize(data: Any?): String? {
                val vals = data as Array<*>?
                /* stopTimestamp, distance, splitTime, travelTime, strokeCount */
                return String.format(Locale.US, "%d,%f,%d,%d,%d", *vals!!)
            }

            public override fun doParse(s: String): Any? {
                val tokens = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                /* stopTimestamp, distance, splitTime, travelTime, strokeCount */
                return arrayOf<Any>(
                    tokens[0],
                    tokens[1], tokens[2], tokens[3], tokens[4]
                )
            }
        }),
        ROWING_START_TRIGGERED,
        ROWING_START(false, LONG()),
        ROWING_COUNT(false, INT()),
        PARAMETER_CHANGE(false, PARAMETER()),
        SESSION_PARAMETER(true, PARAMETER()),
        STROKE_POWER_END(false, true, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf<String>("power")

            override fun exportData(data: Any?): Array<Any> {
                return arrayOf(data!!)
            }
        }, FLOAT()),
        STROKE_RATE(false, true, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf<String>("stroke_rate")

            override fun exportData(data: Any?): Array<Any> {
                return arrayOf(data!!)
            }
        }, INT()),
        STROKE_DECELERATION_TRESHOLD,
        STROKE_ACCELERATION_TRESHOLD,
        STROKE_ROLL(false, FLOAT_ARR()),
        RECOVERY_ROLL(false, FLOAT_ARR()),
        ACCEL(true, false, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf<String>("x", "y", "z")

            override fun exportData(data: Any?): Array<Any>? {
                val af = data as FloatArray

                return arrayOf(af[0], af[1], af[2])
            }
        }, FLOAT_ARR()),
        ORIENT(true, false, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf<String>("azimuth", "pitch", "roll")

            override fun exportData(data: Any?): Array<Any> {
                val af = data as FloatArray

                return arrayOf(af[0], af[1], af[2])
            }
        }, FLOAT_ARR()),
        LOCATION(true, false, object : DataExporter {
            // 0-time (long),
            // 1-lat (double), 2-long (double), 3-alt (double)
            // 4-speed (float), 5-bearing (float), 6-accuracy (float)
            override val columnNames: Array<String>
                get() = arrayOf<String>(
                    "time", "lat", "long", "alt", "speed", "bearing", "accuracy"
                )

            override fun exportData(data: Any?): Array<Any> {
                val a = data as Array<*>

                return arrayOf(
                    a[0]!!, a[1]!!, a[2]!!, a[3]!!, a[4]!!, a[5]!!, a[6]!!
                )
            }
        }, object : DataRecordSerializer() {
            override fun doSerialize(data: Any?): String {
                val vals = data as Array<*>?
                return String.format(Locale.US, "%d,%f,%f,%f,%f,%f,%f", *vals!!)
            }

            public override fun doParse(s: String): Any {
                val tokens = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                return arrayOf<Any>(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6])
            }
        }),
        GPS(true, false, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf<String>("lat", "long", "alt", "speed", "bearing", "accuracy")

            override fun exportData(data: Any?): Array<Any>? {
                val ad = data as DoubleArray

                return arrayOf(
                    ad[0],
                    ad[1], ad[2], ad[3], ad[4], ad[5]
                )
            }
        }, DOUBLE_ARR()),
        WAY(false, true, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf<String>("distance", "speed", "accuracy")

            override fun exportData(data: Any?): Array<Any> {
                val ad = data as DoubleArray

                return arrayOf(ad[0], ad[1], ad[2])
            }
        }, DOUBLE_ARR()),
        ACCUM_DISTANCE(true, DOUBLE()),
        FREEZE_TILT(true, BOOLEAN()),
        HEART_BPM(true, INT()),
        IMMEDIATE_DISTANCE_REQUESTED,
        BOOKMARKED_DISTANCE(false, DISTANCE()),
        ROWING_START_DISTANCE(false, DISTANCE()),
        CRASH_STACK,
        INPUT_START,
        INPUT_STOP,
        REPLAY_PROGRESS,
        REPLAY_SKIPPED,
        REPLAY_PAUSED,
        REPLAY_PLAYING,
        LOGFILE_VERSION(false, INT());

//        private class DistanceEventSerializer : DataRecordSerializer() {
//            override fun doSerialize(data: Any?): String {
//                val vals = data as Array<*>?
//                /* travelTime, travelDistance */
//                return String.format(Locale.US, "%d,%f", *vals!!)
//            }
//
//            public override fun doParse(s: String): Any {
//                val tokens = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
//                    .toTypedArray()
//                return arrayOf<Any>(tokens[0], tokens[1])
//            }
//        }

//        interface DataExporter {
//            val columnNames: Array<String?>?
//
//            fun exportData(data: Any?): Array<Any>?
//        }

        val isParsableEvent: Boolean = dataParser != null

        val isExportableEvent: Boolean = dataExporter != null

        constructor(isReplayableEvent: Boolean, dataParser: DataRecordSerializer) : this(
            isReplayableEvent,
            true,
            null,
            dataParser
        )
    }

    override fun toString(): String {
        val sdata = dataToString()

        return "$type $timestamp $sdata"
    }

    fun dataToString(): String {
        val sdata = if (type.dataParser != null) {
            type.dataParser.serialize(data)
        } else {
            data.toString()
        }
        return sdata
    }

    fun exportData(): Array<Any>? {
        if (type.isExportableEvent) {
            return type.dataExporter!!.exportData(data)
        }

        return null
    }

    companion object {
        fun create(type: Type, timestamp: Long, data: Any?): DataRecord {
            return DataRecord(type, timestamp, data)
        }

        fun create(type: Type, timestamp: Long, str: String?): DataRecord {
            if (type.dataParser != null) {
                val data = type.dataParser.parse(str!!)
                return create(type, timestamp, data)
            } else {
                throw UnsupportedOperationException(
                    String.format(
                        "StrokeEvent type %s does not have a serializer configured",
                        type
                    )
                )
            }
        }

        fun nullableAnyArray(data: Any?): Array<Any>? {
            return if (data != null) {
                arrayOf(data)
            } else {
                null
            }
        }

    }
}