package com.example.paddlestroke.data


class DataRecord(val type: Type, val timestamp: Long, val data: Any) {

    enum class Type(
        val isReplayable: Boolean = false,
        val isViewData: Boolean = true,
        val dataExporter: DataExporter? = null,
        val dataParser: DataRecordSerializer? = null
    ) {
        UUID,
        RECORDING_START,
        RECORDING_COUNTDOWN(false, object : DataRecordSerializer() {
            override fun doSerialize(data: Any?): String? {
                val arr = nullableAnyArray(data)!!
                /* tag, countdown */return String.format("%s,%d", *arr)
            }

            override fun doParse(s: String): Any? {
                val tokens = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                /* tag, countdown */return arrayOf<Any>(
                    tokens[0],
                    tokens[1]
                )
            }
        }),
        STROKE_DROP_BELOW_ZERO,
        STROKE_RISE_ABOVE_ZERO,
        STROKE_POWER_START,
        ROWING_STOP(false, object : DataRecordSerializer() {
            override fun doSerialize(data: Any?): String? {
                val arr = nullableAnyArray(data)!!
                /* stopTimestamp, distance, splitTime, travelTime, strokeCount */return String.format(
                    "%d,%f,%d,%d,%d",
                    *arr
                )
            }

            override fun doParse(s: String): Any {
                val tokens = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                /* stopTimestamp, distance, splitTime, travelTime, strokeCount */return arrayOf<Any>(
                    tokens[0], tokens[1], tokens[2], tokens[3], tokens[4]
                )
            }
        }),
        ROWING_START_TRIGGERED,
        ROWING_START(false, DataRecordSerializer.LONG()),
        ROWING_COUNT(false, DataRecordSerializer.INT()),

        //        PARAMETER_CHANGE(false, PARAMETER()),
//        SESSION_PARAMETER(true, PARAMETER()),
        STROKE_POWER_END(false, true, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf("power")

            override fun exportData(data: Any?): Array<Any>? {
                return if (data != null) {
                    arrayOf(data)
                } else {
                    null
                }
            }
        }, DataRecordSerializer.FLOAT()),
        STROKE_RATE(false, true, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf("stroke_rate")

            override fun exportData(data: Any?): Array<Any>? {
                return if (data != null) {
                    arrayOf(data)
                } else {
                    null
                }
            }
        }, DataRecordSerializer.INT()),
        STROKE_DECELERATION_TRESHOLD,
        STROKE_ACCELERATION_TRESHOLD,
        STROKE_ROLL(false, DataRecordSerializer.FLOAT_ARR()),
        RECOVERY_ROLL(false, DataRecordSerializer.FLOAT_ARR()),

        /**
         * data: FloatArray 0-x, 1-y, 2-z
         */
        ACCEL(true, false, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf("x", "y", "z")

            override fun exportData(data: Any?): Array<Any> {
                val arr = data as FloatArray
                return arrayOf(arr[0], arr[1], arr[2])
            }
        }, DataRecordSerializer.FLOAT_ARR()),
        ORIENT(true, false, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf("azimuth", "pitch", "roll")

            override fun exportData(data: Any?): Array<Any>? {
                val arr = data as FloatArray
                return arrayOf(arr[0], arr[1], arr[2])
            }
        }, DataRecordSerializer.FLOAT_ARR()),

        /**
         * data: DoubleArray
         *  0- "lat"
         *  1- "long"
         *  2- "alt"
         *  3- "speed"
         *  4- "bearing"
         *  5- "accuracy"
         */
        LOCATION(true, false, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf("lat", "long", "alt", "speed", "bearing", "accuracy")

            override fun exportData(data: Any?): Array<Any> {
                val arr = data as DoubleArray
                return arrayOf(
                    arr[0],
                    arr[1], arr[2], arr[3], arr[4], arr[5]
                )
            }
        }, DataRecordSerializer.DOUBLE_ARR()),
        WAY(false, true, object : DataExporter {
            override val columnNames: Array<String>
                get() = arrayOf("distance", "speed", "accuracy")

            override fun exportData(data: Any?): Array<Any>? {
                val arr = data as DoubleArray
                return arrayOf(arr[0], arr[1], arr[2])
            }
        }, DataRecordSerializer.DOUBLE_ARR()),
        ACCUM_DISTANCE(true, DataRecordSerializer.DOUBLE()),
        FREEZE_TILT(true, DataRecordSerializer.BOOLEAN()),
        HEART_BPM(true, DataRecordSerializer.INT()),
        IMMEDIATE_DISTANCE_REQUESTED,
        BOOKMARKED_DISTANCE(false, DataRecordSerializer.DISTANCE()),
        ROWING_START_DISTANCE(false, DataRecordSerializer.DISTANCE()),
        CRASH_STACK,
        INPUT_START,
        INPUT_STOP,
        REPLAY_PROGRESS,
        REPLAY_SKIPPED,
        REPLAY_PAUSED,
        REPLAY_PLAYING,
        LOGFILE_VERSION(false, DataRecordSerializer.INT());

        val isParsableEvent: Boolean
        val isExportableEvent: Boolean

        constructor(isReplayable: Boolean, dataParser: DataRecordSerializer) : this(
            isReplayable,
            true,
            null,
            dataParser
        )

        init {
            isExportableEvent = dataExporter != null
            isParsableEvent = dataParser != null
        }
    }

    override fun toString(): String {
        val str = dataToString()
        return "$type $timestamp $str"
    }

    fun dataToString(): String {
        return if (type.dataParser != null) {
            type.dataParser.serialize(data)
        } else {
            data.toString()
        }
    }

    fun exportData(): Array<Any>? {
        return if (type.isExportableEvent) {
            type.dataExporter!!.exportData(data)
        } else null
    }

    companion object {
        fun create(type: Type, timestamp: Long, data: Any): DataRecord {
            return DataRecord(type, timestamp, data)
        }

        fun create(type: Type, timestamp: Long, str: String): DataRecord {
            return if (type.dataParser != null) {
                val data = type.dataParser.parse(str)!!
                create(type, timestamp, data)
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