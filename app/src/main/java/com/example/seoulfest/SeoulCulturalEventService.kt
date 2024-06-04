package com.example.seoulfest.network

import com.example.seoulfest.models.SeoulCulturalEventResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SeoulCulturalEventService {
    @GET("{apiKey}/{type}/{service}/{startIndex}/{endIndex}")
    suspend fun getEvents(
        @Path("apiKey") apiKey: String,
        @Path("type") type: String,
        @Path("service") service: String,
        @Path("startIndex") startIndex: Int,
        @Path("endIndex") endIndex: Int,
        @Query("DATE") date: String
    ): SeoulCulturalEventResponse

    companion object {
        private const val BASE_URL = "http://openapi.seoul.go.kr:8088/"

        fun create(): SeoulCulturalEventService {
            val logging = HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
                .create(SeoulCulturalEventService::class.java)
        }
    }
}
