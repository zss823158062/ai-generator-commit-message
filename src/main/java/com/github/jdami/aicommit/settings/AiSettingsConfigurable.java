package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.settings.model.ProviderSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AiSettingsConfigurable implements Configurable {

    private AiSettingsComponent settingsComponent;

    @Override
    public String getDisplayName() {
        return "AI Commit Message Generator";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new AiSettingsComponent();
        reset();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsComponent == null) return false;

        AiSettingsState settings = AiSettingsState.getInstance();
        return settingsComponent.getProvider() != settings.provider
                || !settingsComponent.getOllamaEndpoint().equals(settings.providers.ollama.endpoint)
                || !settingsComponent.getOllamaModel().equals(settings.providers.ollama.model)
                || !settingsComponent.getOpenAiEndpoint().equals(settings.providers.openAi.endpoint)
                || !settingsComponent.getOpenAiModel().equals(settings.providers.openAi.model)
                || !settingsComponent.getOpenAiApiKey().equals(settings.providers.openAi.apiKey)
                || !settingsComponent.getOpenRouterEndpoint().equals(settings.providers.openRouter.endpoint)
                || !settingsComponent.getOpenRouterModel().equals(settings.providers.openRouter.model)
                || !settingsComponent.getOpenRouterApiKey().equals(settings.providers.openRouter.apiKey)
                || settingsComponent.getTimeout() != settings.timeout
                || settingsComponent.getMaxDiffChars() != settings.maxDiffChars
                || !settingsComponent.getSystemPrompt().equals(settings.systemPrompt);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsComponent == null) return;

        AiSettingsState settings = AiSettingsState.getInstance();
        AiSettingsState.Provider provider = settingsComponent.getProvider();

        String endpoint = settingsComponent.getOllamaEndpoint().trim();
        String ollamaModel = settingsComponent.getOllamaModel().trim();
        String systemPrompt = settingsComponent.getSystemPrompt().trim();
        String openAiEndpoint = settingsComponent.getOpenAiEndpoint().trim();
        String openAiModel = settingsComponent.getOpenAiModel().trim();
        String openAiApiKey = settingsComponent.getOpenAiApiKey().trim();
        String openRouterEndpoint = settingsComponent.getOpenRouterEndpoint().trim();
        String openRouterModel = settingsComponent.getOpenRouterModel().trim();
        String openRouterApiKey = settingsComponent.getOpenRouterApiKey().trim();

        if (systemPrompt.isEmpty()) {
            throw new ConfigurationException("System prompt cannot be empty");
        }

        if (provider == AiSettingsState.Provider.OLLAMA) {
            if (endpoint.isEmpty()) throw new ConfigurationException("Ollama endpoint cannot be empty");
            if (ollamaModel.isEmpty()) throw new ConfigurationException("Ollama model cannot be empty");
        } else if (provider == AiSettingsState.Provider.OPENAI) {
            if (openAiEndpoint.isEmpty()) throw new ConfigurationException("OpenAI API base cannot be empty");
            if (openAiModel.isEmpty()) throw new ConfigurationException("OpenAI model cannot be empty");
            if (openAiApiKey.isEmpty()) throw new ConfigurationException("OpenAI API key cannot be empty");
        } else if (provider == AiSettingsState.Provider.OPENROUTER) {
            if (openRouterEndpoint.isEmpty()) throw new ConfigurationException("OpenRouter API base cannot be empty");
            if (openRouterModel.isEmpty()) throw new ConfigurationException("OpenRouter model cannot be empty");
            if (openRouterApiKey.isEmpty()) throw new ConfigurationException("OpenRouter API key cannot be empty");
        }

        settings.provider = provider != null ? provider : AiSettingsState.Provider.OLLAMA;

        ProviderSettings providers = settings.providers != null ? settings.providers : new ProviderSettings();
        providers.ollama.endpoint = endpoint;
        providers.ollama.model = ollamaModel;
        providers.openAi.endpoint = openAiEndpoint;
        providers.openAi.model = openAiModel;
        providers.openAi.apiKey = openAiApiKey;
        providers.openRouter.endpoint = openRouterEndpoint;
        providers.openRouter.model = openRouterModel;
        providers.openRouter.apiKey = openRouterApiKey;
        settings.providers = providers;

        // Keep legacy fields in sync
        settings.ollamaEndpoint = endpoint;
        settings.ollamaModel = ollamaModel;
        settings.modelName = ollamaModel;
        settings.openAiEndpoint = openAiEndpoint;
        settings.openAiModel = openAiModel;
        settings.openAiApiKey = openAiApiKey;

        settings.timeout = settingsComponent.getTimeout();
        settings.maxDiffChars = settingsComponent.getMaxDiffChars();
        settings.systemPrompt = systemPrompt;

        settings.loadState(settings);
    }

    @Override
    public void reset() {
        if (settingsComponent == null) return;

        AiSettingsState settings = AiSettingsState.getInstance();
        settingsComponent.setProvider(settings.provider != null ? settings.provider : AiSettingsState.Provider.OLLAMA);

        ProviderSettings providers = settings.providers != null ? settings.providers : new ProviderSettings();
        settingsComponent.setOllamaEndpoint(providers.ollama != null && providers.ollama.endpoint != null ? providers.ollama.endpoint : "http://localhost:11434");
        settingsComponent.setOllamaModel(providers.ollama != null && providers.ollama.model != null ? providers.ollama.model : "qwen3:8b");
        settingsComponent.setOpenAiEndpoint(providers.openAi != null && providers.openAi.endpoint != null ? providers.openAi.endpoint : "https://api.openai.com");
        settingsComponent.setOpenAiModel(providers.openAi != null && providers.openAi.model != null ? providers.openAi.model : "gpt-4o-mini");
        settingsComponent.setOpenAiApiKey(providers.openAi != null && providers.openAi.apiKey != null ? providers.openAi.apiKey : "");
        settingsComponent.setOpenRouterEndpoint(providers.openRouter != null && providers.openRouter.endpoint != null ? providers.openRouter.endpoint : "https://openrouter.ai/api");
        settingsComponent.setOpenRouterModel(providers.openRouter != null && providers.openRouter.model != null ? providers.openRouter.model : "anthropic/claude-3.5-sonnet");
        settingsComponent.setOpenRouterApiKey(providers.openRouter != null && providers.openRouter.apiKey != null ? providers.openRouter.apiKey : "");

        settingsComponent.setTimeout(settings.timeout);
        settingsComponent.setMaxDiffChars(settings.maxDiffChars);
        settingsComponent.setSystemPrompt(settings.systemPrompt != null ? settings.systemPrompt : new AiSettingsState().systemPrompt);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
