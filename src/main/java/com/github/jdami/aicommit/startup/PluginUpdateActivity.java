package com.github.jdami.aicommit.startup;

import com.github.jdami.aicommit.settings.AiSettingsState;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginUpdateActivity implements StartupActivity, ProjectActivity {

    private static final String PLUGIN_ID = "com.github.jdami.ai-generator-commit-message";

    @Override
    public void runActivity(@NotNull Project project) {
        run(project);
    }

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        run(project);
        return Unit.INSTANCE;
    }

    private void run(Project project) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        if (plugin == null) return;

        String currentVersion = plugin.getVersion();
        AiSettingsState settings = AiSettingsState.getInstance();

        if (!currentVersion.equals(settings.pluginVersion)) {
            settings.resetToDefaults();
            settings.pluginVersion = currentVersion;
        }
    }
}
