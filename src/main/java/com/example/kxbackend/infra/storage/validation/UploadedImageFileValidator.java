package com.example.kxbackend.infra.storage.validation;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class UploadedImageFileValidator {

    public static final long MAX_IMAGE_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    private static final String ALLOWED_FORMAT_MESSAGE = "PNG, JPG, WEBP";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Map<String, ImageFormat> FORMAT_BY_CONTENT_TYPE = Map.of(
            "image/png", ImageFormat.PNG,
            "image/jpeg", ImageFormat.JPEG,
            "image/webp", ImageFormat.WEBP
    );
    private static final Map<String, ImageFormat> FORMAT_BY_EXTENSION = Map.of(
            "png", ImageFormat.PNG,
            "jpg", ImageFormat.JPEG,
            "jpeg", ImageFormat.JPEG,
            "webp", ImageFormat.WEBP
    );

    static {
        ImageIO.scanForPlugins();
    }

    private UploadedImageFileValidator() {
    }

    public static void validate(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, label + " 파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, label + "는 50MB 이하만 업로드할 수 있습니다.");
        }

        ImageFormat extensionFormat = validateExtension(file, label);
        ImageFormat contentTypeFormat = validateContentType(file, label);
        ImageFormat actualFormat = validateMagicBytes(file, label);

        if (extensionFormat != actualFormat || contentTypeFormat != actualFormat) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    label + "의 확장자, Content-Type, 실제 파일 형식이 일치하지 않습니다."
            );
        }

        validateDecodableImage(file, label);
    }

    private static ImageFormat validateExtension(MultipartFile file, String label) {
        String extension = extractExtension(file.getOriginalFilename());
        if (!StringUtils.hasText(extension) || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    label + "는 " + ALLOWED_FORMAT_MESSAGE + " 형식만 업로드할 수 있습니다."
            );
        }
        return FORMAT_BY_EXTENSION.get(extension);
    }

    private static ImageFormat validateContentType(MultipartFile file, String label) {
        String contentType = file.getContentType();
        ImageFormat format = FORMAT_BY_CONTENT_TYPE.get(contentType);
        if (format == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    label + "는 " + ALLOWED_FORMAT_MESSAGE + " 형식만 업로드할 수 있습니다."
            );
        }
        return format;
    }

    private static ImageFormat validateMagicBytes(MultipartFile file, String label) {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, label + " 파일을 읽을 수 없습니다.");
        }

        ImageFormat format = detectFormat(header, read);
        if (format == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    label + " 파일이 손상되었거나 실제 이미지 형식이 아닙니다."
            );
        }
        return format;
    }

    private static void validateDecodableImage(MultipartFile file, String label) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        label + " 파일이 손상되었거나 실제 이미지 형식이 아닙니다."
                );
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    label + " 파일이 손상되었거나 실제 이미지 형식이 아닙니다."
            );
        }
    }

    private static ImageFormat detectFormat(byte[] header, int read) {
        if (read >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return ImageFormat.PNG;
        }
        if (read >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return ImageFormat.JPEG;
        }
        if (read >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) {
            return ImageFormat.WEBP;
        }
        return null;
    }

    private static String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private enum ImageFormat {
        PNG,
        JPEG,
        WEBP
    }
}
