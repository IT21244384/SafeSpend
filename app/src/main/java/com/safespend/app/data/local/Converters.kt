package com.safespend.app.data.local

import androidx.room.TypeConverter
import com.safespend.app.data.model.TxSource
import com.safespend.app.data.model.TxType
import java.time.LocalDate

class Converters {

    /** Epoch day keeps dates comparable with plain SQL integer operators. */
    @TypeConverter
    fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun txTypeToString(value: TxType): String = value.name

    @TypeConverter
    fun stringToTxType(value: String): TxType = TxType.valueOf(value)

    @TypeConverter
    fun txSourceToString(value: TxSource): String = value.name

    @TypeConverter
    fun stringToTxSource(value: String): TxSource = TxSource.valueOf(value)
}
