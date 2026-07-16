package com.orkutclone.api.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Armazena fotos de álbum em disco local, uma subpasta por álbum, e devolve a URL
 * pública do arquivo. Segue a mesma convenção do {@link AvatarStorageService}.
 */
@Service
public class AlbumPhotoStorageService {

    private final Path baseDir;
    private final String publicBaseUrl;

    public AlbumPhotoStorageService(
            @Value("${app.storage.album-dir:uploads/albums}") String albumDir,
            @Value("${app.storage.album-public-base-url:/uploads/albums}") String publicBaseUrl) {
        this.baseDir = Paths.get(albumDir).toAbsolutePath().normalize();
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create album storage directory: " + baseDir, e);
        }
    }

    public String store(UUID albumId, byte[] data, String extension) {
        Path albumDir = baseDir.resolve(albumId.toString()).normalize();
        try {
            Files.createDirectories(albumDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create album directory: " + albumDir, e);
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path target = albumDir.resolve(filename).normalize();
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to store photo file", e);
        }
        return publicBaseUrl + "/" + albumId + "/" + filename;
    }

    /**
     * Remove o arquivo referenciado pela URL. Ignora URLs nulas, externas
     * ou que apontem para fora do diretório de armazenamento.
     */
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String prefix = publicBaseUrl + "/";
        if (!url.startsWith(prefix)) {
            return;
        }
        Path target = baseDir.resolve(url.substring(prefix.length())).normalize();
        if (!target.startsWith(baseDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete photo file", e);
        }
    }

    /** Remove a subpasta inteira do álbum (todas as fotos), usado ao apagar o álbum. */
    public void deleteAlbumDirectory(UUID albumId) {
        Path albumDir = baseDir.resolve(albumId.toString()).normalize();
        if (!albumDir.startsWith(baseDir) || !Files.exists(albumDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(albumDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to delete album file: " + path, e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete album directory: " + albumDir, e);
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/") && value.length() > 1) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
