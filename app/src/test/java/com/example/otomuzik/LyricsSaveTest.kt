package com.example.otomuzik

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.id3.ID3v23Frame
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import org.jaudiotagger.tag.id3.valuepair.TextEncoding
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class LyricsSaveTest {

    @Test
    fun testFileWriteAndRead() {
        // Minimal valid MP3 header
        val tempFile = File.createTempFile("test_song", ".mp3")
        try {
            // Write a dummy empty MP3
            val fos = FileOutputStream(tempFile)
            // ID3v2 header: "ID3", ver 3.0, flags 0, size 0
            fos.write(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0, 0, 0, 0, 0))
            // Dummy MPEG frame sync
            fos.write(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte()))
            fos.write(ByteArray(1000))
            fos.close()

            val af = AudioFileIO.read(tempFile)
            val tag = af.tagOrCreateAndSetDefault
            tag.setField(FieldKey.TITLE, "Test Başlık")
            tag.setField(FieldKey.LYRICS, "Yeni Şarkı Sözü\nİkinci Satır\nŞarkı Sonu")
            af.commit()

            // Read back
            val afRead = AudioFileIO.read(tempFile)
            val lyricsRead = afRead.tag?.getFirst(FieldKey.LYRICS)
            println("Read back lyrics: " + lyricsRead)
            println("Tag class: " + afRead.tag?.javaClass?.name)
        } catch (e: Throwable) {
            println("Exception: " + e.javaClass.name + ": " + e.message)
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }
    }
}
