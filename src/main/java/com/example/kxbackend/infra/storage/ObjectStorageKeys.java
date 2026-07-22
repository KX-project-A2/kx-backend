package com.example.kxbackend.infra.storage;

/**
 * 저장소 object key 규칙
 * - uploads/{userId}/{uuid}.{ext}
 * - openai-images/{userId}/{uuid}.png
 * - videos/{userId}/{uuid}.mp4
 * - profile-images/{userId}/{uuid}.{ext}
 */
public final class ObjectStorageKeys {

    private ObjectStorageKeys() {
    }

    public static String uploadImageKey(Long userId, String fileName) {
        return join("uploads", String.valueOf(userId), fileName);
    }

    public static String generatedImageKey(Long userId, String fileName) {
        return join("openai-images", String.valueOf(userId), fileName);
    }

    public static String generatedVideoKey(Long userId, String fileName) {
        return join("videos", String.valueOf(userId), fileName);
    }

    public static String profileImageKey(Long userId, String fileName) {
        return join("profile-images", String.valueOf(userId), fileName);
    }

    /**
     * DB에 저장된 경로를 S3/로컬 공통 object key로 정규화한다.
     */
    public static String normalize(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        return objectKey.replace('\\', '/').replaceAll("^/+", "");
    }

    public static String fileNameOf(String objectKey) {
        String normalized = normalize(objectKey);
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private static String join(String... parts) {
        return String.join("/", parts);
    }
}
