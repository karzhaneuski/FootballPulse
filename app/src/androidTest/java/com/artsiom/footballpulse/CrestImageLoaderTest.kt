package com.artsiom.footballpulse

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.decode.DataSource
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrestImageLoaderTest {
    @Test
    fun svgAndPngCrestsCanBeReloadedFromDiskWithoutNetwork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val png = ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.RED)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
        val fixtures = listOf(
            "image/svg+xml" to """<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"><rect width="32" height="32" fill="red"/></svg>""".toByteArray(),
            "image/png" to png
        )
        for ((contentType, bytes) in fixtures) {
            // Replace only transport, retaining the application's decoder and cache configuration.
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .sentRequestAtMillis(System.currentTimeMillis())
                    .receivedResponseAtMillis(System.currentTimeMillis())
                    .code(200)
                    .message("OK")
                    .header("Cache-Control", "public, max-age=86400")
                    .header("Content-Type", contentType)
                    .body(bytes.toResponseBody(contentType.toMediaType()))
                    .build()
            }.build()
            val loader = context.imageLoader.newBuilder().okHttpClient(client).build()
            val request = ImageRequest.Builder(context)
                .data("http://crests.example.test/${UUID.randomUUID()}")
                .size(32)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            val online = loader.execute(request)
            assertTrue("Unable to decode $contentType: $online", online is SuccessResult)
            assertEquals(DataSource.NETWORK, (online as SuccessResult).dataSource)
            assertEquals(32, online.drawable.intrinsicWidth)

            val offlineLoader = loader.newBuilder().okHttpClient(
                OkHttpClient.Builder().addInterceptor {
                    throw IOException("Disk cache reload must not access the network")
                }.build()
            ).build()
            val offline = offlineLoader.execute(request.newBuilder()
                .networkCachePolicy(CachePolicy.DISABLED)
                .build())
            assertTrue("Unable to reload $contentType from disk: $offline", offline is SuccessResult)
            assertEquals(DataSource.DISK, (offline as SuccessResult).dataSource)
            assertEquals(32, offline.drawable.intrinsicWidth)
        }
    }
}
