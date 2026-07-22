package com.example.kxbackend.infra.storage;

import java.io.InputStream;

/**
 * 객체 저장소 추상화 (로컬 / S3)
 */
public interface ObjectStorage {

    /**
     * 객체를 저장한다.
     */
    void put(String objectKey, byte[] content, String contentType);

    /**
     * 객체를 스트림으로 저장한다.
     */
    void put(String objectKey, InputStream content, long contentLength, String contentType);

    /**
     * 객체를 조회한다.
     */
    StoredObject get(String objectKey);

    /**
     * 객체 존재 여부를 확인한다.
     */
    boolean exists(String objectKey);

    record StoredObject(byte[] content, String contentType, String fileName) {
    }
}
