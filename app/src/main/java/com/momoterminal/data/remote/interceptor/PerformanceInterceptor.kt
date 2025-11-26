package com.momoterminal.data.remote.interceptor

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor for Firebase Performance Monitoring.
 * Automatically tracks HTTP request/response metrics.
 */
@Singleton
class PerformanceInterceptor @Inject constructor() : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        val httpMetric = createHttpMetric(request)
        httpMetric?.start()
        
        return try {
            val response = chain.proceed(request)
            
            httpMetric?.apply {
                setHttpResponseCode(response.code)
                setResponseContentType(response.header(HEADER_CONTENT_TYPE))
                setResponsePayloadSize(response.body?.contentLength() ?: 0L)
                stop()
            }
            
            response
        } catch (e: IOException) {
            httpMetric?.stop()
            throw e
        }
    }

    /**
     * Create an HttpMetric for the request.
     * @param request The OkHttp request
     * @return HttpMetric or null if URL is invalid
     */
    private fun createHttpMetric(request: Request): HttpMetric? {
        val url = request.url.toString()
        val method = request.method
        
        return try {
            val httpMethod = when (method.uppercase()) {
                "GET" -> FirebasePerformance.HttpMethod.GET
                "POST" -> FirebasePerformance.HttpMethod.POST
                "PUT" -> FirebasePerformance.HttpMethod.PUT
                "DELETE" -> FirebasePerformance.HttpMethod.DELETE
                "PATCH" -> FirebasePerformance.HttpMethod.PATCH
                "HEAD" -> FirebasePerformance.HttpMethod.HEAD
                "OPTIONS" -> FirebasePerformance.HttpMethod.OPTIONS
                "TRACE" -> FirebasePerformance.HttpMethod.TRACE
                "CONNECT" -> FirebasePerformance.HttpMethod.CONNECT
                else -> FirebasePerformance.HttpMethod.GET
            }
            
            val metric = FirebasePerformance.getInstance().newHttpMetric(url, httpMethod)
            
            // Set request payload size if available
            request.body?.contentLength()?.let { size ->
                if (size > 0) {
                    metric.setRequestPayloadSize(size)
                }
            }
            
            metric
        } catch (_: Exception) {
            // Return null if metric creation fails
            null
        }
    }

    companion object {
        private const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
