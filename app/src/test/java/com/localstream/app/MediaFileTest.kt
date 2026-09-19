package com.localstream.app

import com.localstream.app.domain.model.MediaFile
import com.localstream.app.domain.model.MediaType
import com.localstream.app.storage.FileTypeResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFileTest {

    @Test
    fun testFormattedSize() {
        val fileBytes = MediaFile(
            name = "test.txt",
            path = "/test.txt",
            uriString = "",
            mimeType = "text/plain",
            size = 500L,
            modifiedDate = 0L,
            folder = "test",
            mediaType = MediaType.DOCUMENT
        )
        assertEquals("500.0 B", fileBytes.formattedSize)

        val fileMb = fileBytes.copy(size = 10485760L) // 10MB
        assertEquals("10.0 MB", fileMb.formattedSize)
    }

    @Test
    fun testFormattedDuration() {
        val media = MediaFile(
            name = "song.mp3",
            path = "/song.mp3",
            uriString = "",
            mimeType = "audio/mpeg",
            size = 1000L,
            modifiedDate = 0L,
            folder = "Music",
            mediaType = MediaType.AUDIO,
            durationMs = 125000L // 2m 05s
        )
        assertEquals("02:05", media.formattedDuration)

        val longMedia = media.copy(durationMs = 3665000L) // 1h 01m 05s
        assertEquals("1:01:05", longMedia.formattedDuration)
    }

    @Test
    fun testFileTypeResolver() {
        assertEquals(MediaType.VIDEO, FileTypeResolver.resolveMediaType("movie.mp4"))
        assertEquals(MediaType.VIDEO, FileTypeResolver.resolveMediaType("clip.mkv"))
        assertEquals(MediaType.AUDIO, FileTypeResolver.resolveMediaType("track.flac"))
        assertEquals(MediaType.IMAGE, FileTypeResolver.resolveMediaType("photo.webp"))
        assertEquals(MediaType.DOCUMENT, FileTypeResolver.resolveMediaType("doc.pdf"))
    }
}
