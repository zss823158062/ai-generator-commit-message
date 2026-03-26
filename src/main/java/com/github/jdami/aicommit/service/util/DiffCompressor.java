package com.github.jdami.aicommit.service.util;

import com.github.jdami.aicommit.settings.AiSettingsState;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public final class DiffCompressor {

    private DiffCompressor() {
    }

    @NotNull
    public static String compress(@NotNull String diffContent) {
        AiSettingsState settings = AiSettingsState.getInstance();
        int maxChars = settings.maxDiffChars;

        if (maxChars <= 0 || diffContent.length() <= maxChars) {
            return diffContent;
        }

        StringBuilder compressed = new StringBuilder(diffContent.length() / 2);
        try (BufferedReader reader = new BufferedReader(new StringReader(diffContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("diff --git") || line.startsWith("index")
                        || line.startsWith("---") || line.startsWith("+++")
                        || line.startsWith("@@") || line.startsWith("File:")
                        || line.startsWith("Operation:")) {
                    compressed.append(line).append('\n');
                    continue;
                }
                if (line.startsWith("+") || line.startsWith("-")) {
                    compressed.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            return diffContent;
        }

        return compressed.toString();
    }
}
