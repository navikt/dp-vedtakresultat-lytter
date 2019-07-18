package no.nav.dp.vedtakslytter

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val moshiInstance: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .add(InstantAdapter())
    .add(ZonedDateTimeAdapter())
    .build()!!

class InstantAdapter {
    @ToJson
    fun toJson(instant: Instant): String {
        return instant.toString()
    }

    @FromJson
    fun fromJson(json: String): Instant {
        return Instant.parse(json)
    }
}

class ZonedDateTimeAdapter {
    @ToJson
    fun toJson(zonedDateTime: ZonedDateTime): String {
        return zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }

    @FromJson
    fun fromJson(json: String): ZonedDateTime {
        return ZonedDateTime.parse(json)
    }
}
