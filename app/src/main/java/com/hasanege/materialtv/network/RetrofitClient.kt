package com.hasanege.materialtv.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import java.security.cert.CertificateException
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.hasanege.materialtv.MainApplication

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

            val cacheInterceptor = Interceptor { chain ->
                var request = chain.request()
                // Fetch from cache for up to 24 hours if network fails, or always check for updates
                // For a faster experience, we'll force cache usage for categories/vod if recent
                request = request.newBuilder()
                    .header("Cache-Control", "public, max-age=60") // Cache for 60 seconds by default
                    .build()
                chain.proceed(request)
            }

            // Setup cache: 50 MB
            val cacheSize = (50 * 1024 * 1024).toLong()
            val cache = Cache(File(MainApplication.instance.cacheDir, "http_cache"), cacheSize)

            val loggingInterceptor = Interceptor { chain ->
                val request = chain.request()
                val sanitizedUrl = com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(request.url.toString())
                android.util.Log.d("RetrofitClient", "Request: ${request.method} $sanitizedUrl")
                val response = try {
                    chain.proceed(request)
                } catch (e: Exception) {
                    android.util.Log.e("RetrofitClient", "Request failed: $sanitizedUrl | Error: ${e.javaClass.simpleName}")
                    throw e
                }
                android.util.Log.d("RetrofitClient", "Response: ${response.code} for $sanitizedUrl")
                response
            }

            OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(headersInterceptor)
                .addNetworkInterceptor(cacheInterceptor)
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
}

