package com.github.senx.aicommit.service.util;

public final class CommitMessageCleaner {

    private static final String[] UNWANTED_PREFIXES = {
            "Based on the git diff",
            "Here's the analysis",
            "This message:",
            "You can use this",
            "The suggested commit message",
            "Here is the suggested commit message",
            "Analysis of the changes:",
            "```",
            "Here's the commit message:",
            "Commit message:"
    };

    private static final String[] UNWANTED_SUFFIXES = {
            "This message:",
            "You can use this message",
            "This commit message",
            "The above message"
    };

    private static final String[] COMMIT_TYPES = {
            "feat(", "fix(", "docs(", "style(", "refactor(", "test(", "chore("
    };

    private CommitMessageCleaner() {
    }

    public static String clean(String rawResponse) {
        String cleaned = removeThinkTags(rawResponse);
        return extractCommitMessage(cleaned);
    }

    public static String removeThinkTags(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?is)<think>.*?</think>", "").trim();
    }

    public static String extractCommitMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text.trim();

        // Remove unwanted prefixes
        for (String prefix : UNWANTED_PREFIXES) {
            if (cleaned.toLowerCase().startsWith(prefix.toLowerCase())) {
                cleaned = cleaned.substring(prefix.length()).trim();
                cleaned = cleaned.replaceFirst("^[:\\-\\s]+", "").trim();
            }
        }

        // Remove code blocks
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "").trim();
        cleaned = cleaned.replaceAll("```.*", "").trim();

        // Remove unwanted suffixes
        for (String suffix : UNWANTED_SUFFIXES) {
            int index = cleaned.toLowerCase().indexOf(suffix.toLowerCase());
            if (index > 0) {
                cleaned = cleaned.substring(0, index).trim();
            }
        }

        // Extract commit message lines
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundCommitStart = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (!foundCommitStart && isCommitMessageLine(line)) {
                foundCommitStart = true;
                result.append(line).append("\n");
            } else if (foundCommitStart) {
                if (line.startsWith("- ") || isCommitMessageLine(line)) {
                    result.append(line).append("\n");
                } else if (line.toLowerCase().contains("this message")
                        || line.toLowerCase().contains("you can use")
                        || line.toLowerCase().contains("analysis")) {
                    break;
                }
            }
        }

        return result.toString().trim();
    }

    private static boolean isCommitMessageLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String trimmed = line.trim();
        for (String type : COMMIT_TYPES) {
            if (trimmed.toLowerCase().startsWith(type)) {
                return true;
            }
        }
        return trimmed.startsWith("- ");
    }
}
