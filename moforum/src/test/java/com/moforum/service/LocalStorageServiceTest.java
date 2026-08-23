package com.moforum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService service() {
        return new LocalStorageService(tempDir.toString());
    }

    @Test
    void uploadWritesFileAndReturnsPublicUrl() throws Exception {
        LocalStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "fake-image-bytes".getBytes());

        String url = service.upload(file);

        assertTrue(url.startsWith("/uploads/"));
        String key = url.substring("/uploads/".length());
        Path saved = tempDir.resolve(key);
        assertTrue(Files.exists(saved));
        assertEquals("fake-image-bytes", new String(Files.readAllBytes(saved)));
    }

    @Test
    void extractKeyReturnsFilenameForValidUrl() {
        LocalStorageService service = service();
        assertEquals("abc123.jpg", service.extractKey("/uploads/abc123.jpg"));
    }

    @Test
    void extractKeyRejectsInvalidUrl() {
        LocalStorageService service = service();
        assertNull(service.extractKey("/uploads/../../etc/passwd"));
        assertNull(service.extractKey("/uploads/../secret.txt"));
        assertNull(service.extractKey("/uploads/a/b.txt"));
        assertNull(service.extractKey("https://bucket.oss.com/uploads/a.jpg"));
        assertNull(service.extractKey(null));
        assertNull(service.extractKey(""));
    }

    @Test
    void deleteByUrlRemovesFile() throws Exception {
        LocalStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "x".getBytes());
        String url = service.upload(file);
        String key = service.extractKey(url);

        service.delete(url);

        assertFalse(Files.exists(tempDir.resolve(key)));
    }

    @Test
    void deleteByKeyCannotEscapeRoot() throws Exception {
        Path root = tempDir.resolve("uploads");
        Files.createDirectories(root);
        LocalStorageService service = new LocalStorageService(root.toString());

        Path sensitive = tempDir.resolve("secret.txt"); // root 之外的兄弟文件
        Files.write(sensitive, "keep".getBytes());

        service.deleteByKey("../secret.txt"); // 若未防护会越界删除 sensitive

        assertTrue(Files.exists(sensitive));
    }

    @Test
    void uploadRejectsDisallowedExtension() {
        LocalStorageService service = service();

        MockMultipartFile html = new MockMultipartFile(
                "file", "payload.html", "image/png", "fake".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.upload(html));

        MockMultipartFile svg = new MockMultipartFile(
                "file", "x.svg", "image/svg+xml", "fake".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.upload(svg));

        MockMultipartFile noext = new MockMultipartFile(
                "file", "noext", "image/png", "fake".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.upload(noext));
    }

    @Test
    void uploadAcceptsUppercaseExtension() throws Exception {
        LocalStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.PNG", "image/png", "fake-image-bytes".getBytes());

        String url = service.upload(file);

        assertTrue(url.endsWith(".png"));
    }
}
