package com.example.myapplicationtoday

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class GenerateAudioTest {

    @Test
    fun generateRawFocusAudioTracks() {
        val rawDir = File("src/main/res/raw")
        if (!rawDir.exists()) rawDir.mkdirs()

        createWavFile(File(rawDir, "focus_track_1.wav"), 216.0, 432.0, 5, "ambient")
        createWavFile(File(rawDir, "focus_track_2.wav"), 150.0, 300.0, 5, "rain")
        createWavFile(File(rawDir, "focus_track_3.wav"), 528.0, 535.0, 5, "binaural")

        assert(File(rawDir, "focus_track_1.wav").exists())
        assert(File(rawDir, "focus_track_2.wav").exists())
        assert(File(rawDir, "focus_track_3.wav").exists())
    }

    private fun createWavFile(file: File, freq1: Double, freq2: Double, durationSec: Int, type: String) {
        val sampleRate = 22050
        val numSamples = sampleRate * durationSec
        val dataSize = numSamples * 2
        val fileSize = 36 + dataSize

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // RIFF Header
        dos.writeBytes("RIFF")
        dos.writeInt(Integer.reverseBytes(fileSize))
        dos.writeBytes("WAVE")

        // fmt Subchunk
        dos.writeBytes("fmt ")
        dos.writeInt(Integer.reverseBytes(16)) // Subchunk1Size
        dos.writeShort(java.lang.Short.reverseBytes(1).toInt()) // AudioFormat = 1 (PCM)
        dos.writeShort(java.lang.Short.reverseBytes(1).toInt()) // NumChannels = 1 (Mono)
        dos.writeInt(Integer.reverseBytes(sampleRate))
        dos.writeInt(Integer.reverseBytes(sampleRate * 2)) // ByteRate
        dos.writeShort(java.lang.Short.reverseBytes(2).toInt()) // BlockAlign
        dos.writeShort(java.lang.Short.reverseBytes(16).toInt()) // BitsPerSample

        // data Subchunk
        dos.writeBytes("data")
        dos.writeInt(Integer.reverseBytes(dataSize))

        val random = java.util.Random()
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val fade = sin(PI * i / numSamples)

            val v = when (type) {
                "ambient" -> (sin(2 * PI * freq1 * t) * 0.6 + sin(2 * PI * freq2 * t) * 0.4) * fade
                "rain" -> ((random.nextDouble() * 2.0 - 1.0) * 0.3 + sin(2 * PI * freq1 * t) * 0.3) * fade
                else -> (sin(2 * PI * freq1 * t) * 0.5 + sin(2 * PI * (freq1 + 7) * t) * 0.5) * fade
            }

            val sample = (v.coerceIn(-1.0, 1.0) * 16000).toInt().toShort()
            dos.writeShort(java.lang.Short.reverseBytes(sample).toInt())
        }

        dos.flush()
        file.writeBytes(baos.toByteArray())
    }
}
