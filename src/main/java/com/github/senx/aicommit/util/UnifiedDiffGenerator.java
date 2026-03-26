package com.github.senx.aicommit.util;

import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public class UnifiedDiffGenerator {

    public static String generateDiff(@NotNull Change change, @NotNull Project project) {
        try {
            List<Change> changes = Collections.singletonList(change);
            List patches = IdeaTextPatchBuilder.buildPatch(project, changes, project.getBasePath(), false);
            if (patches.isEmpty()) {
                return "";
            }
            StringWriter writer = new StringWriter();
            UnifiedDiffWriter.write(project, patches, writer, "\n", null);
            return writer.toString();
        } catch (VcsException e) {
            return "Error generating diff: " + e.getMessage();
        } catch (Exception e) {
            return "Failed to generate diff for file: " +
                    (change.getVirtualFile() != null ? change.getVirtualFile().getName() : "unknown");
        }
    }
}
