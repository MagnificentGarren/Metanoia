package com.example.myapplicationtoday

import org.junit.Test
import java.io.File

class GenerateAudioTest {

    @Test
    fun verifyCuratedFocusAudioAssetsExist() {
        val assetsDir = File("src/main/assets")
        assert(assetsDir.exists())

        val beatA = File(assetsDir, "lo-fi_beat_A.mp3")
        val beatB = File(assetsDir, "lo-fi_beat_B.mp3")
        val beatC = File(assetsDir, "lo-fi_beat_C.mp3")
        val trapBeat = File(assetsDir, "trap_beat_1.mp3")

        assert(beatA.exists()) { "lo-fi_beat_A.mp3 missing!" }
        assert(beatB.exists()) { "lo-fi_beat_B.mp3 missing!" }
        assert(beatC.exists()) { "lo-fi_beat_C.mp3 missing!" }
        assert(trapBeat.exists()) { "trap_beat_1.mp3 missing!" }
    }
}
