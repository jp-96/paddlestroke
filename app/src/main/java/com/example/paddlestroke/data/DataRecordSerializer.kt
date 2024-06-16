package com.example.paddlestroke.data


abstract class DataRecordSerializer {

    private val STR_NULL_VALUE = "(null)"

    fun parse(s: String): Any? {
        return if (s == STR_NULL_VALUE) {
            null
        } else {
            doParse(s)
        }
    }

    fun serialize(data: Any?): String {
        return data?.let { doSerialize(it) } ?: STR_NULL_VALUE
    }

    protected open fun doSerialize(data: Any?): String? {
        return data.toString()
    }

    protected abstract fun doParse(s: String): Any?

    class BOOLEAN : DataRecordSerializer() {
        override fun doParse(s: String): Any? {
            return s.toBooleanStrictOrNull()
        }
    }

    class LONG : DataRecordSerializer() {
        override fun doParse(s: String): Any? {
            return s.toLongOrNull()
        }
    }

    class INT : DataRecordSerializer() {
        override fun doParse(s: String): Any? {
            return s.toIntOrNull()
        }
    }

    class FLOAT : DataRecordSerializer() {
        override fun doParse(s: String): Any? {
            return s.toFloatOrNull()
        }
    }

    class DOUBLE : DataRecordSerializer() {
        override fun doParse(s: String): Any? {
            return s.toDoubleOrNull()
        }
    }

    class FLOAT_ARR @JvmOverloads constructor(private val separator: String = ",") :
        DataRecordSerializer() {
        override fun doParse(s: String): Any? {
            val strings = s.split(separator.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val res = FloatArray(strings.size)
            for (i in strings.indices) {
                res[i] = strings[i].toFloat()
            }
            return res
        }

        override fun doSerialize(data: Any?): String? {
            if (data == null) return null
            var s = ""
            var i = 0
            for (f in data as FloatArray) {
                if (i++ != 0) {
                    s += separator
                }
                s += f
            }
            return s
        }
    }

    class DOUBLE_ARR @JvmOverloads constructor(private val separator: String = ",") :
        DataRecordSerializer() {
        override fun doParse(s: String): Any? {
            val strings = s.split(separator.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val res = DoubleArray(strings.size)
            for (i in strings.indices) {
                res[i] = strings[i].toDouble()
            }
            return res
        }

        override fun doSerialize(data: Any?): String? {
            if (data == null) return null
            var s = ""
            var i = 0
            for (f in data as DoubleArray) {
                if (i++ != 0) {
                    s += separator
                }
                s += f
            }
            return s
        }
    }

    class DISTANCE : DataRecordSerializer() {
        override fun doSerialize(data: Any?): String {
            val arr = DataRecord.nullableAnyArray(data)!!
            /* travelTime, travelDistance */return String.format("%d,%f", *arr)
        }

        override fun doParse(s: String): Any {
            val tokens = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            return arrayOf<Any>(tokens[0], tokens[1])
        }
    }

//    class PARAMETER : DataRecordSerializer() {
//        override fun doParse(s: String?): Any {
//            return ParameterBusEventData(s)
//        }
//    }

}
