package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.github.jdami.aicommit.service.provider.OllamaProviderClient;
import com.github.jdami.aicommit.service.provider.OpenAiProviderClient;
import com.github.jdami.aicommit.service.provider.OpenRouterProviderClient;
import com.github.jdami.aicommit.settings.model.ProviderSettings;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class AiSettingsComponent {

    private final JPanel mainPanel;
    private final JComboBox<AiSettingsState.Provider> providerCombo = new JComboBox<>(AiSettingsState.Provider.values());
    private final JPanel providerCards = new JPanel(new CardLayout());

    private final JBTextField ollamaEndpointField = new JBTextField();
    private final JBTextField ollamaModelField = new JBTextField();

    private final JBTextField openAiEndpointField = new JBTextField();
    private final JBTextField openAiModelField = new JBTextField();
    private final JBPasswordField openAiApiKeyField = new JBPasswordField();

    private final JBTextField openRouterEndpointField = new JBTextField();
    private final JBTextField openRouterModelField = new JBTextField();
    private final JBPasswordField openRouterApiKeyField = new JBPasswordField();

    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
    private final JTextArea systemPromptArea = new JTextArea(5, 40);

    public AiSettingsComponent() {
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(systemPromptArea);

        providerCards.add(buildOllamaPanel(), AiSettingsState.Provider.OLLAMA.name());
        providerCards.add(buildOpenAiPanel(), AiSettingsState.Provider.OPENAI.name());
        providerCards.add(buildOpenRouterPanel(), AiSettingsState.Provider.OPENROUTER.name());

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Provider: "), providerCombo, 1, false)
                .addComponent(providerCards)
                .addLabeledComponent(new JBLabel("Timeout (seconds): "), timeoutSpinner, 1, false)
                .addLabeledComponent(new JBLabel("System Prompt: "), scrollPane, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
        providerCombo.addActionListener(e -> switchProviderCard());
    }

    private JPanel buildOllamaPanel() {
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(AiSettingsState.Provider.OLLAMA));
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Ollama Endpoint: "), ollamaEndpointField, 1, false)
                .addLabeledComponent(new JBLabel("Ollama Model: "), ollamaModelField, 1, false)
                .addLabeledComponent(new JBLabel(""), testButton, 1, false)
                .getPanel();
    }

    private JPanel buildOpenAiPanel() {
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(AiSettingsState.Provider.OPENAI));
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenAI API Base: "), openAiEndpointField, 1, false)
                .addLabeledComponent(new JBLabel("OpenAI Model: "), openAiModelField, 1, false)
                .addLabeledComponent(new JBLabel("OpenAI API Key: "), openAiApiKeyField, 1, false)
                .addLabeledComponent(new JBLabel(""), testButton, 1, false)
                .getPanel();
    }

    private JPanel buildOpenRouterPanel() {
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(AiSettingsState.Provider.OPENROUTER));
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenRouter API Base: "), openRouterEndpointField, 1, false)
                .addLabeledComponent(new JBLabel("OpenRouter Model: "), openRouterModelField, 1, false)
                .addLabeledComponent(new JBLabel("OpenRouter API Key: "), openRouterApiKeyField, 1, false)
                .addLabeledComponent(new JBLabel(""), testButton, 1, false)
                .getPanel();
    }

    public JPanel getPanel() { return mainPanel; }

    public AiSettingsState.Provider getProvider() {
        return (AiSettingsState.Provider) providerCombo.getSelectedItem();
    }

    public void setProvider(AiSettingsState.Provider provider) {
        providerCombo.setSelectedItem(provider);
        switchProviderCard();
    }

    public void setProviders(ProviderSettings providers) {
        if (providers == null) return;
        setOllamaEndpoint(providers.ollama != null ? providers.ollama.endpoint : "");
        setOllamaModel(providers.ollama != null ? providers.ollama.model : "");
        setOpenAiEndpoint(providers.openAi != null ? providers.openAi.endpoint : "");
        setOpenAiModel(providers.openAi != null ? providers.openAi.model : "");
        setOpenAiApiKey(providers.openAi != null ? providers.openAi.apiKey : "");
        setOpenRouterEndpoint(providers.openRouter != null ? providers.openRouter.endpoint : "");
        setOpenRouterModel(providers.openRouter != null ? providers.openRouter.model : "");
        setOpenRouterApiKey(providers.openRouter != null ? providers.openRouter.apiKey : "");
    }

    public String getOllamaEndpoint() { return ollamaEndpointField.getText() != null ? ollamaEndpointField.getText() : ""; }
    public void setOllamaEndpoint(String endpoint) { ollamaEndpointField.setText(endpoint != null ? endpoint : ""); }

    public String getOllamaModel() { return ollamaModelField.getText() != null ? ollamaModelField.getText() : ""; }
    public void setOllamaModel(String model) { ollamaModelField.setText(model != null ? model : ""); }

    public int getTimeout() { return (Integer) timeoutSpinner.getValue(); }
    public void setTimeout(int timeout) { timeoutSpinner.setValue(timeout); }

    public String getOpenAiEndpoint() { return openAiEndpointField.getText() != null ? openAiEndpointField.getText() : ""; }
    public void setOpenAiEndpoint(String endpoint) { openAiEndpointField.setText(endpoint != null ? endpoint : ""); }

    public String getOpenAiModel() { return openAiModelField.getText() != null ? openAiModelField.getText() : ""; }
    public void setOpenAiModel(String model) { openAiModelField.setText(model != null ? model : ""); }

    public String getOpenAiApiKey() { return openAiApiKeyField.getPassword() != null ? String.valueOf(openAiApiKeyField.getPassword()) : ""; }
    public void setOpenAiApiKey(String apiKey) { openAiApiKeyField.setText(apiKey != null ? apiKey : ""); }

    public String getSystemPrompt() { return systemPromptArea.getText() != null ? systemPromptArea.getText() : ""; }
    public void setSystemPrompt(String prompt) { systemPromptArea.setText(prompt != null ? prompt : ""); }

    public String getOpenRouterEndpoint() { return openRouterEndpointField.getText() != null ? openRouterEndpointField.getText() : ""; }
    public void setOpenRouterEndpoint(String endpoint) { openRouterEndpointField.setText(endpoint != null ? endpoint : ""); }

    public String getOpenRouterModel() { return openRouterModelField.getText() != null ? openRouterModelField.getText() : ""; }
    public void setOpenRouterModel(String model) { openRouterModelField.setText(model != null ? model : ""); }

    public String getOpenRouterApiKey() { return openRouterApiKeyField.getPassword() != null ? String.valueOf(openRouterApiKeyField.getPassword()) : ""; }
    public void setOpenRouterApiKey(String apiKey) { openRouterApiKeyField.setText(apiKey != null ? apiKey : ""); }

    private void switchProviderCard() {
        CardLayout layout = (CardLayout) providerCards.getLayout();
        AiSettingsState.Provider provider = getProvider() != null ? getProvider() : AiSettingsState.Provider.OLLAMA;
        layout.show(providerCards, provider.name());
    }

    private void testConnection(AiSettingsState.Provider provider) {
        String endpoint;
        String model;
        String apiKey;

        switch (provider) {
            case OLLAMA:
                endpoint = getOllamaEndpoint().trim();
                model = getOllamaModel().trim();
                apiKey = "";
                break;
            case OPENAI:
                endpoint = getOpenAiEndpoint().trim();
                model = getOpenAiModel().trim();
                apiKey = getOpenAiApiKey().trim();
                break;
            case OPENROUTER:
                endpoint = getOpenRouterEndpoint().trim();
                model = getOpenRouterModel().trim();
                apiKey = getOpenRouterApiKey().trim();
                break;
            default:
                throw new IllegalStateException("Unknown provider: " + provider);
        }

        if (endpoint.isEmpty()) {
            Messages.showErrorDialog("Endpoint cannot be empty", "Test Connection Failed");
            return;
        }
        if (model.isEmpty()) {
            Messages.showErrorDialog("Model cannot be empty", "Test Connection Failed");
            return;
        }
        if ((provider == AiSettingsState.Provider.OPENAI || provider == AiSettingsState.Provider.OPENROUTER) && apiKey.isEmpty()) {
            Messages.showErrorDialog("API Key cannot be empty", "Test Connection Failed");
            return;
        }

        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                GenerationInputs inputs = new GenerationInputs(
                        "Test connection",
                        "You are a test assistant. Reply with 'OK' if you receive this message.",
                        endpoint, model, apiKey, getTimeout()
                );

                String response;
                switch (provider) {
                    case OLLAMA:
                        response = new OllamaProviderClient().generate(inputs, null);
                        break;
                    case OPENAI:
                        response = new OpenAiProviderClient().generate(inputs, null);
                        break;
                    case OPENROUTER:
                        response = new OpenRouterProviderClient().generate(inputs, null);
                        break;
                    default:
                        throw new IllegalStateException("Unknown provider: " + provider);
                }

                String finalResponse = response;
                SwingUtilities.invokeLater(() -> Messages.showInfoMessage(
                        "Connection successful!\n\nProvider: " + provider +
                                "\nModel: " + model +
                                "\nResponse: " + finalResponse.substring(0, Math.min(100, finalResponse.length())) + "...",
                        "Test Connection Successful"
                ));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> Messages.showErrorDialog(
                        "Connection failed: " + ex.getMessage(),
                        "Test Connection Failed"
                ));
            }
        }, "Testing Connection...", true, null);
    }
}
