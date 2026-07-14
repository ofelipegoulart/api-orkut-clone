package com.orkutclone.api.support;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Bytes of an uploaded image that already passed validation, together with the extension
 * resolved from the bytes themselves.
 *
 * <p>The format is decided by the actual content and never by the declared Content-Type,
 * which is what prevents MIME type spoofing.</p>
 */
public record UploadedImage(byte[] data, String extension) {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final int MIN_DIMENSION = 32;
    private static final Set<String> ALLOWED_FORMATS = Set.of("png", "jpg", "gif", "bmp");

    public static UploadedImage from(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No image file provided");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file");
        }

        if (data.length > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image exceeds maximum size of 10MB");
        }

        return new UploadedImage(data, resolveExtension(data));
    }

    private static String resolveExtension(byte[] data) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (iis == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read image data");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid image format. Supported: PNG, JPG, GIF, BMP");
            }
            ImageReader reader = readers.next();
            try {
                String extension = normalizeExtension(reader.getFormatName());
                if (!ALLOWED_FORMATS.contains(extension)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid image format. Supported: PNG, JPG, GIF, BMP");
                }
                reader.setInput(iis);
                if (reader.getWidth(0) < MIN_DIMENSION || reader.getHeight(0) < MIN_DIMENSION) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be at least 32x32 pixels");
                }
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to process image");
        }
    }

    private static String normalizeExtension(String imageType) {
        String type = imageType.toLowerCase();
        return type.equals("jpeg") ? "jpg" : type;
    }
}
