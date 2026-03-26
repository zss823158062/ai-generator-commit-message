package com.github.senx.aicommit.service.util;

import com.github.senx.aicommit.settings.AiSettingsState;
import org.jetbrains.annotations.NotNull;

public final class DiffTruncator {

    private static final int PROMPT_TEMPLATE_OVERHEAD = 800;
    private static final int RESPONSE_RESERVE = 2000;

    private DiffTruncator() {
    }

    @NotNull
    public static String truncate(@NotNull String diffContent) {
        AiSettingsState settings = AiSettingsState.getInstance();
        int maxTotalChars = settings.maxDiffChars;
        int systemPromptLength = settings.systemPrompt != null ? settings.systemPrompt.length() : 0;
        int totalOverhead = systemPromptLength + PROMPT_TEMPLATE_OVERHEAD + RESPONSE_RESERVE;
        int maxDiffChars = maxTotalChars - totalOverhead;

        if (maxTotalChars <= 0) {
            return diffContent;
        }

        if (maxDiffChars <= 0) {
            maxDiffChars = 5000;
        }

        if (diffContent.length() <= maxDiffChars) {
            return diffContent;
        }

        int cutPoint = findLineBreakBefore(diffContent, maxDiffChars);
        String truncated = diffContent.substring(0, cutPoint);
        int removedChars = diffContent.length() - cutPoint;

        String truncationMessage = String.format(
                "\n\n[... \u5185\u5bb9\u8fc7\u957f\uff0c\u5df2\u622a\u65ad %,d \u5b57\u7b26 (\u539f\u59cb %,d \u5b57\u7b26, \u4fdd\u7559 %.1f%%) ...]",
                removedChars, diffContent.length(),
                (double) truncated.length() * 100.0 / (double) diffContent.length());

        return truncated + truncationMessage;
    }

    private static int findLineBreakBefore(String content, int maxPosition) {
        if (maxPosition >= content.length()) {
            return content.length();
        }
        int lastNewline = content.lastIndexOf('\n', maxPosition);
        if (lastNewline > 0) {
            return lastNewline;
        }
        return maxPosition;
    }

    public static int estimateTokens(int charCount) {
        return (int) Math.ceil((double) charCount / 3.5);
    }

    @NotNull
    public static String getStats(@NotNull String diffContent) {
        int chars = diffContent.length();
        int lines = diffContent.split("\n").length;
        int estimatedTokens = estimateTokens(chars);
        return String.format("\u5b57\u7b26\u6570: %,d | \u884c\u6570: %,d | \u9884\u4f30 Token: ~%,d", chars, lines, estimatedTokens);
    }
}
