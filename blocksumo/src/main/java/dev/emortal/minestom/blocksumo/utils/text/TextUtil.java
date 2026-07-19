package dev.emortal.minestom.blocksumo.utils.text;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class TextUtil {
    private static final char[] SMALL_FONT = new char[]{
            'ᴀ', 'ʙ', 'ᴄ', 'ᴅ', 'ᴇ', 'ꜰ', 'ɢ', 'ʜ', 'ɪ', 'ᴊ', 'ᴋ', 'ʟ', 'ᴍ', 'ɴ', 'ᴏ', 'ᴘ', 'ǫ', 'ʀ', 'ѕ', 'ᴛ', 'ᴜ',
            'ᴠ', 'ᴡ', 'х', 'ʏ', 'ᴢ', '₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉'
    };

    public static @NotNull String smallFont(@NotNull String input) {
        StringBuilder builder = new StringBuilder();
        String lowercase = input.toLowerCase(Locale.ROOT);

        for (char element : lowercase.toCharArray()) {
            if (element >= 'a' && element <= 'z') {
                builder.append(SMALL_FONT[element - 'a']);
            } else if (element >= '0' && element <= '9') {
                builder.append(SMALL_FONT[element + 26 - '0']);
            } else {
                builder.append(element);
            }
        }

        return builder.toString();
    }

    private TextUtil() {
    }
}
