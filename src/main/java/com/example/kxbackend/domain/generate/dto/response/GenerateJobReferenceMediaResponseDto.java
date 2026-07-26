package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.entity.GenerateJobReferenceMedia;
import com.example.kxbackend.domain.generate.entity.enums.ReferenceImageType;
import com.example.kxbackend.domain.media.entity.MediaFile;

public record GenerateJobReferenceMediaResponseDto(
        Long mediaFileId,
        ReferenceImageType referenceType,
        int referenceOrder,
        String filePath
) {

    public static GenerateJobReferenceMediaResponseDto from(GenerateJobReferenceMedia referenceMedia) {
        MediaFile mediaFile = referenceMedia.getMediaFile();
        return new GenerateJobReferenceMediaResponseDto(
                mediaFile.getId(),
                referenceMedia.getReferenceType(),
                referenceMedia.getReferenceOrder(),
                mediaFile.getFilePath()
        );
    }
}
