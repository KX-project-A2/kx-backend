package com.example.kxbackend.domain.generate.prompt;

import com.example.kxbackend.domain.generate.dto.request.CharacterConceptSheetRequestDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 구조화된 캐릭터 JSON을 공식 캐릭터 설정표(Concept Art Sheet)용 OpenAI 프롬프트로 조립한다.
 */
@Component
public class CharacterConceptArtPromptBuilder {

    private static final String NONE_ACCESSORY = "없음";

    /**
     * 캐릭터 설정표 생성용 최종 프롬프트를 반환한다.
     */
    public String build(CharacterConceptSheetRequestDto request) {
        List<String> resolvedAccessories = resolveAccessories(request.accessories());

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                Create a single, official Character Concept Art Sheet image.
                Adapt mood and presentation to the given art style and world view — not limited to any specific medium or IP genre.
                Clean white / light-grey professional grid layout with thin divider lines.
                Section headers and labels should be readable (Korean preferred for UI labels).
                The image must be ONE cohesive sheet containing exactly these six regions:

                [Area A - Left Profile Panel]
                - Invent a fitting character name and one-line intro from the visual attributes below
                - Vertical profile table derived from the selected traits (age band, gender, body type, world view, personality implied by expression/outfit)
                - Bottom row of circular keyword icon chips inferred from the character concept
                - Do NOT leave Area A empty; enrich it creatively while staying consistent with the input traits

                [Area B - Top Center Full-body Turnaround]
                - Same character shown as Front / Side / Back full-body standing turnaround
                - Clearly show body type, outfit structure, and worn accessories simultaneously

                [Area C - Top Right Expression Guide]
                - 3 to 6 close-up face chips of the SAME character
                - Center around the base expression, then vary to smile, determination, anger, surprise, worry, indifference, etc.

                [Area D - Bottom Left Detail Shots]
                - 3 to 4 square framed close-ups of outfit parts, shoes, emblems, and distinctive details

                [Area E - Bottom Center Color Palette & Items]
                - Color palette swatches with HEX codes as square chips
                - Standalone object graphics for EVERY accessory item below, each with name/short description

                [Area F - Bottom Right Concept & World]
                - Background atmosphere matching the world view
                - Small dynamic pose / silhouette cuts of the character

                Visual consistency rules:
                - Same face, body, hair, eyes, outfit, and accessories across every panel
                - Apply the requested art style and world-view mood consistently
                - No collage of unrelated characters; one official setting sheet only
                """.stripIndent());

        prompt.append("\n\n=== CHARACTER INPUT DATA ===\n");
        appendIfPresent(prompt, "gender", request.gender());
        appendIfPresent(prompt, "age", request.age());
        appendIfPresent(prompt, "body_type", request.bodyType());
        appendIfPresent(prompt, "art_style", request.artStyle());
        appendIfPresent(prompt, "world_view", request.worldView());
        appendIfPresent(prompt, "hair_length", request.hairLength());
        appendIfPresent(prompt, "hair_style", request.hairStyle());
        appendIfPresent(prompt, "hair_color", request.hairColor());
        appendIfPresent(prompt, "eye_color", request.eyeColor());
        appendIfPresent(prompt, "eye_characteristic", request.eyeCharacteristic());
        appendIfPresent(prompt, "expression", request.expression());
        appendIfPresent(prompt, "outfit_genre", request.outfitGenre());
        appendIfPresent(prompt, "outfit_color", request.outfitColor());
        appendAccessories(prompt, resolvedAccessories);

        prompt.append("""

                === CRITICAL ACCESSORY RULE ===
                The character must visually wear/carry ALL listed accessories at the same time in turnaround and detail shots.
                Area E must render a separate standalone icon for EACH listed accessory.
                """);

        if (resolvedAccessories.isEmpty()) {
            prompt.append("""
                    No accessories were selected. Leave Area E item section mostly empty or use subtle default equipment only.
                    Do NOT invent major accessories.
                    """);
        }

        prompt.append("""

                Output only the finished official Character Concept Art Sheet visual.
                """);

        return prompt.toString().trim();
    }

    /**
     * "없음"과 다른 아이템이 함께 있으면 "없음"을 제거하고, 그 외에는 유효 악세서리만 반환한다.
     */
    List<String> resolveAccessories(List<String> accessories) {
        if (accessories == null || accessories.isEmpty()) {
            return List.of();
        }

        List<String> cleaned = accessories.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();

        boolean hasRealItem = cleaned.stream().anyMatch(value -> !isNoneAccessory(value));
        if (!hasRealItem) {
            return List.of();
        }

        List<String> resolved = new ArrayList<>();
        for (String accessory : cleaned) {
            if (!isNoneAccessory(accessory) && !resolved.contains(accessory)) {
                resolved.add(accessory);
            }
        }
        return List.copyOf(resolved);
    }

    private boolean isNoneAccessory(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return NONE_ACCESSORY.equals(value.trim())
                || "none".equals(normalized)
                || "없음".equals(value.trim());
    }

    private void appendIfPresent(StringBuilder prompt, String key, String value) {
        if (value != null && !value.isBlank()) {
            prompt.append(key).append(": ").append(value.trim()).append('\n');
        }
    }

    private void appendAccessories(StringBuilder prompt, List<String> accessories) {
        if (accessories.isEmpty()) {
            prompt.append("accessories: []\n");
            return;
        }
        prompt.append("accessories: [")
                .append(accessories.stream().map(item -> "\"" + item + "\"").collect(Collectors.joining(", ")))
                .append("]\n");
    }
}
