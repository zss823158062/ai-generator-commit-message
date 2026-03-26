package com.github.jdami.aicommit.actions;

import com.github.jdami.aicommit.service.AiService;
import com.github.jdami.aicommit.util.UnifiedDiffGenerator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateCommitMessageAction extends AnAction {

    private AiService aiService;
    private volatile boolean isGenerating;
    private volatile boolean wasCancelled;
    private volatile ProgressIndicator currentIndicator;
    private final Object stateLock = new Object();

    private AiService getAiService() {
        if (aiService == null) {
            aiService = new AiService();
        }
        return aiService;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        if (isGenerating) {
            cancelGeneration();
            return;
        }

        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (!(data instanceof CheckinProjectPanel)) {
            Messages.showErrorDialog(project, "Unable to access commit panel", "Error");
            return;
        }

        CheckinProjectPanel checkinPanel = (CheckinProjectPanel) data;
        CommitMessageI commitMessageI = checkinPanel;
        Collection<Change> changes = checkinPanel.getSelectedChanges();

        FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);
        @SuppressWarnings("unchecked")
        Collection<VirtualFile> selectedVirtualFiles = checkinPanel.getVirtualFiles();

        List<FilePath> unversionedFiles = selectedVirtualFiles.stream()
                .filter(vf -> {
                    FileStatus status = fileStatusManager.getStatus(vf);
                    boolean isUnversioned = status == FileStatus.UNKNOWN;
                    String vfPath = vf.getPath();
                    boolean notInChanges = changes.stream().noneMatch(c ->
                            (c.getAfterRevision() != null && c.getAfterRevision().getFile().getPath().equals(vfPath))
                                    || (c.getBeforeRevision() != null && c.getBeforeRevision().getFile().getPath().equals(vfPath)));
                    return isUnversioned && notInChanges;
                })
                .map(VcsUtil::getFilePath)
                .collect(Collectors.toList());

        if (changes.isEmpty() && unversionedFiles.isEmpty()) {
            Messages.showWarningDialog(project, "No changes selected for commit", "Warning");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating commit message...", true) {
            private String generatedMessage;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                markGenerating(indicator);
                try {
                    indicator.setText("Analyzing changes...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.3);
                    indicator.checkCanceled();

                    String diffContent = getDiffContent(project, changes, unversionedFiles);
                    if (diffContent == null || diffContent.trim().isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showWarningDialog(project, "No diff content found", "Warning"));
                        return;
                    }

                    indicator.setText("Calling AI service...");
                    indicator.setFraction(0.6);
                    indicator.checkCanceled();

                    generatedMessage = getAiService().generateCommitMessage(diffContent, indicator);
                    indicator.setFraction(1.0);
                } catch (ProcessCanceledException canceled) {
                    wasCancelled = true;
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog(project,
                                    "Failed to generate commit message: " + ex.getMessage(), "Error"));
                }
            }

            @Override
            public void onSuccess() {
                if (generatedMessage != null && !generatedMessage.isEmpty() && !wasCancelled) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            commitMessageI.setCommitMessage(generatedMessage));
                }
            }

            @Override
            public void onCancel() {
                wasCancelled = true;
            }

            @Override
            public void onFinished() {
                markIdle();
            }
        });
    }

    private String getDiffContent(Project project, Collection<Change> changes, List<FilePath> unversionedFiles) {
        StringBuilder diffBuilder = new StringBuilder();

        for (Change change : changes) {
            String diff = UnifiedDiffGenerator.generateDiff(change, project);
            if (!diff.isEmpty()) {
                diffBuilder.append(diff);
            }
        }

        for (FilePath filePath : unversionedFiles) {
            try {
                String diff = generateDiffForUnversionedFile(filePath);
                if (!diff.isEmpty()) {
                    diffBuilder.append(diff);
                }
            } catch (Exception e) {
                // Skip files that fail
            }
        }

        String finalDiff = diffBuilder.toString();
        return finalDiff.isEmpty() ? null : finalDiff;
    }

    private String generateDiffForUnversionedFile(FilePath filePath) throws Exception {
        String absolutePath = filePath.getPath();
        String relativePath = normalizePathForDiff(absolutePath);
        File file = new File(absolutePath);

        if (!file.exists() || !file.isFile()) {
            return "";
        }

        if (filePath.getFileType().isBinary()) {
            String fileType = filePath.getFileType().getName();
            if ("UNKNOWN".equals(fileType)) {
                int lastDotIndex = absolutePath.lastIndexOf('.');
                if (lastDotIndex > 0 && lastDotIndex < absolutePath.length() - 1) {
                    fileType = absolutePath.substring(lastDotIndex + 1).toUpperCase() + " File";
                }
            }
            return String.format("File: %s (%s)\nOperation: New File\n", relativePath, fileType);
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String[] lines = content.split("\\r?\\n", -1);
            StringBuilder diff = new StringBuilder();
            diff.append("diff --git a/").append(relativePath).append(" b/").append(relativePath).append("\n");
            diff.append("new file mode 100644\n");
            diff.append("--- /dev/null\n");
            diff.append("+++ b/").append(relativePath).append("\n");
            diff.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");
            for (String line : lines) {
                diff.append("+").append(line).append("\n");
            }
            return diff.toString();
        } catch (MalformedInputException e) {
            String fileType = "Binary";
            int lastDotIndex = absolutePath.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < absolutePath.length() - 1) {
                fileType = absolutePath.substring(lastDotIndex + 1).toUpperCase() + " File";
            }
            return String.format("File: %s (%s)\nOperation: New File\n", relativePath, fileType);
        } catch (Exception e) {
            return String.format("File: %s (Unknown Type)\nOperation: New File\nNote: Error reading content\n", relativePath);
        }
    }

    private String normalizePathForDiff(String path) {
        return path.replace('\\', '/');
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
        e.getPresentation().setIcon(IconLoader.getIcon(
                isGenerating ? "/icons/aiCommitStop.svg" : "/icons/aiCommit.svg",
                GenerateCommitMessageAction.class));
        e.getPresentation().setText(isGenerating ? "\u505c\u6b62\u751f\u6210" : "Commit\u52a9\u624b");
        e.getPresentation().setDescription(isGenerating
                ? "\u505c\u6b62\u751f\u6210 commit message"
                : "\u4f7f\u7528AI\u751f\u6210 commit message");
    }

    private void markGenerating(ProgressIndicator indicator) {
        synchronized (stateLock) {
            isGenerating = true;
            wasCancelled = false;
            currentIndicator = indicator;
        }
    }

    private void markIdle() {
        synchronized (stateLock) {
            isGenerating = false;
            currentIndicator = null;
        }
        getAiService().cancelOngoingCall();
    }

    private void cancelGeneration() {
        synchronized (stateLock) {
            if (!isGenerating) return;
            if (currentIndicator != null) {
                currentIndicator.cancel();
            }
            wasCancelled = true;
        }
        getAiService().cancelOngoingCall();
    }
}
