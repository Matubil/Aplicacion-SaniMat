package com.sanimat.util;

/**
   Normaliza textos antes de guardar, por ejemplo nombres en formato capitalizado
 */
public final class TextNormalizer {
    private TextNormalizer() {
    }

    public static String capitalizeWords(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return normalized;
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (Character.isLetter(current)) {
                builder.append(capitalizeNext ? Character.toTitleCase(current) : Character.toLowerCase(current));
                capitalizeNext = false;
            } else {
                builder.append(current);
                capitalizeNext = Character.isWhitespace(current) || current == '-' || current == '\'';
            }
        }
        return builder.toString();
    }
}
