package no.nav.dp.vedtakslytter

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant

val moshiInstance: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .add(InstantAdapter())
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