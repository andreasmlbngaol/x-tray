package id.andreasmlbngaol.xtray.platform.notifications

import org.freedesktop.dbus.types.Variant
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationIconResolverTest {

    @Test
    fun `decodes gdkpixbuf rgba image data`() {
        val image = decode(
            pixels = solid(200, 150, 120, 255),
            channels = 4,
            hasAlpha = true,
        )

        assertPixel(image, 200, 150, 120, 255)
    }

    @Test
    fun `decodes spec argb image data`() {
        val image = decode(
            pixels = solid(255, 200, 150, 120),
            channels = 4,
            hasAlpha = true,
        )

        assertPixel(image, 200, 150, 120, 255)
    }

    @Test
    fun `decodes opaque rgba without transparency`() {
        val image = decode(
            pixels = solid(0, 0, 0, 255),
            channels = 4,
            hasAlpha = false,
        )

        assertPixel(image, 0, 0, 0, 255)
    }

    @Test
    fun `decodes three channel rgb`() {
        val image = decode(
            pixels = solid(200, 150, 120),
            channels = 3,
            hasAlpha = false,
        )

        assertPixel(image, 200, 150, 120, 255)
    }

    private fun decode(pixels: ByteArray, channels: Int, hasAlpha: Boolean): BufferedImage {
        val width = 2
        val height = 2
        val payload = listOf<Any>(
            width,
            height,
            width * channels,
            hasAlpha,
            8,
            channels,
            pixels,
        )
        val bytes = NotificationIconResolver.resolve(
            appIcon = "",
            hints = mapOf("image-data" to Variant(payload, "(iiibiiay)")),
        ) ?: error("resolver returned no icon")
        return ImageIO.read(ByteArrayInputStream(bytes))
    }

    private fun solid(vararg channels: Int): ByteArray {
        val pixel = ByteArray(channels.size) { channels[it].toByte() }
        return ByteArray(pixel.size * 4) { pixel[it % pixel.size] }
    }

    private fun assertPixel(image: BufferedImage, red: Int, green: Int, blue: Int, alpha: Int) {
        val argb = image.getRGB(0, 0)
        assertEquals(alpha, (argb ushr 24) and 0xFF)
        assertEquals(red, (argb ushr 16) and 0xFF)
        assertEquals(green, (argb ushr 8) and 0xFF)
        assertEquals(blue, argb and 0xFF)
    }
}
