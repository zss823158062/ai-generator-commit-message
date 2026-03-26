package com.github.jdami.aicommit.actions;

import com.github.jdami.aicommit.service.AiService;
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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.Refreshable;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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

        if (changes.isEmpty()) {
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

                    String diffContent = getDiffContent(project, changes);
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

    private String getDiffContent(Project project, Collection<Change> changes) {
        try {
            GitRepository repository = GitUtil.getRepositoryManager(project)
                    .getRepositories().stream().findFirst().orElse(null);
            if (repository == null) {
                return null;
            }

            String repoPath = repository.getRoot().getPath();
            StringBuilder diffBuilder = new StringBuilder();

            for (Change change : changes) {
                String absolutePath = null;
                boolean isNewFile = change.getBeforeRevision() == null;
                boolean isDeletedFile = change.getAfterRevision() == null;

                if (change.getAfterRevision() != null) {
                    absolutePath = change.getAfterRevision().getFile().getPath();
                } else if (change.getBeforeRevision() != null) {
                    absolutePath = change.getBeforeRevision().getFile().getPath();
                }

                if (absolutePath == null) continue;

                String relativePath = absolutePath;
                if (absolutePath.startsWith(repoPath)) {
                    relativePath = absolutePath.substring(repoPath.length());
                    if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                        relativePath = relativePath.substring(1);
                    }
                }

                try {
                    String fileDiff = null;

                    if (isNewFile) {
                        fileDiff = executeGitDiff(repoPath, "diff", "--cached", "HEAD", "--", relativePath);
                        if (fileDiff.isEmpty()) {
                            fileDiff = executeGitDiff(repoPath, "diff", "--cached", "--", relativePath);
                        }
                        if (fileDiff.isEmpty()) {
                            String showContent = executeGitDiff(repoPath, "show", ":" + relativePath);
                            if (!showContent.isEmpty()) {
                                String[] contentLines = showContent.split("\n");
                                fileDiff = "diff --git a/" + relativePath + " b/" + relativePath +
                                        "\nnew file mode 100644\n--- /dev/null\n+++ b/" + relativePath +
                                        "\n@@ -0,0 +1," + contentLines.length + " @@\n";
                                for (String line : contentLines) {
                                    fileDiff += "+" + line + "\n";
                                }
                            }
                        }
                    } else if (isDeletedFile) {
                        fileDiff = executeGitDiff(repoPath, "diff", "--cached", "--", relativePath);
                        if (fileDiff.isEmpty()) {
                            fileDiff = executeGitDiff(repoPath, "diff", "HEAD", "--", relativePath);
                        }
                    } else {
                        fileDiff = executeGitDiff(repoPath, "diff", "--cached", "--", relativePath);
                        if (fileDiff.isEmpty()) {
                            fileDiff = executeGitDiff(repoPath, "diff", "--", relativePath);
                        }
                        if (fileDiff.isEmpty()) {
                            fileDiff = executeGitDiff(repoPath, "diff", "HEAD", "--", relativePath);
                        }
                    }

                    if (fileDiff != null && !fileDiff.isEmpty()) {
                        diffBuilder.append(fileDiff);
                    }
                } catch (Exception e) {
                    // Skip files that fail to diff
                }
            }

            String finalDiff = diffBuilder.toString();
            return finalDiff.isEmpty() ? null : finalDiff;
        } catch (Exception e) {
            return "Unable to get diff content: " + e.getMessage();
        }
    }

    private String executeGitDiff(String repoPath, String... args) throws Exception {
        ArrayList<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(args));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(repoPath));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return "";
        }
        return output.toString();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
        e.getPresentation().setIcon(IconLoader.getIcon(
                isGenerating ? "/icons/aiCommitStop.svg" : "/icons/aiCommit.svg",
                GenerateCommitMessageAction.class));
        e.getPresentation().setText(isGenerating ? "停止生成" : "Git助手");
        e.getPresentation().setDescription(isGenerating
                ? "停止生成 commit message"
                : "Generate commit message using AI");
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
