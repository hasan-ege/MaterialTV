package com.hasanege.materialtv.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.security.cert.CertificateException
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val unsafeOkHttpClient: OkHttpClient by lazy {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            val headersInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .header("x-requested-with", "com.hasanege.materialtv")
                    .build()
                chain.proceed(requestWithHeaders)
            }

            OkHttpClient.Builder()
                .addInterceptor(headersInterceptor)
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun getClient(baseUrl: String): XtreamApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(unsafeOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(XtreamApiService::class.java)
    }

    fun getImdbSuggestionClient(): ImdbSuggestionApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://v2.sg.media-imdb.com/")
            .client(unsafeOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(ImdbSuggestionApiService::class.java)
    }

    fun getOmdbClient(): OmdbApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(unsafeOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(OmdbApiService::class.java)
    }
}
