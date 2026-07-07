package com.orkutclone.api.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Armazena avatares em disco local e devolve a URL pública do arquivo.
 *
 * <p>O binário decodificado é gravado em {@code app.storage.avatar-dir} e servido
 * estaticamente sob {@code app.storage.public-base-url} (ver {@code WebConfig}).
 * Somente a URL é persistida na coluna {@code users.profile_picture}.</p>
 */
@Service
public class AvatarStorageService {

    private final Path baseDir;
    private final String publicBaseUrl;

    public AvatarStorageService(
            @Value("${app.storage.avatar-dir:uploads/avatars}") String avatarDir,
            @Value("${app.storage.public-base-url:/uploads/avatars}") String publicBaseUrl) {
        this.baseDir = Paths.get(avatarDir).toAbsolutePath().normalize();
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create avatar storage directory: " + baseDir, e);
        }
    }

    /**
     * Grava os bytes com a extensão informada e retorna a URL pública.
     */
    public String store(byte[] data, String extension) {
        String filename = UUID.randomUUID() + "." + extension;
        Path target = baseDir.resolve(filename).normalize();
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to store avatar file", e);
        }
        return publicBaseUrl + "/" + filename;
    }

    /**
     * Remove o arquivo referenciado pela URL. Ignora URLs nulas, externas
     * ou que apontem para fora do diretório de armazenamento.
     */
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash == url.length() - 1) {
            return;
        }
        String filename = url.substring(slash + 1);
        Path target = baseDir.resolve(filename).normalize();
        if (!target.startsWith(baseDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete avatar file", e);
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/") && value.length() > 1) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
