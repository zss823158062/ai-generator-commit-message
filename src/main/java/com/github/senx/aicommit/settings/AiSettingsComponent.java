package com.github.senx.aicommit.settings;

import com.github.senx.aicommit.service.model.GenerationInputs;
import com.github.senx.aicommit.service.provider.OllamaProviderClient;
import com.github.senx.aicommit.service.provider.OpenAiProviderClient;
import com.github.senx.aicommit.service.provider.OpenRouterProviderClient;
import com.github.senx.aicommit.settings.model.ProviderSettings;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
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
    private final JComboBox<String> ollamaModelCombo = createEditableCombo();

    private final JBTextField openAiEndpointField = new JBTextField();
    private final JComboBox<String> openAiModelCombo = createEditableCombo();
    private final JBPasswordField openAiApiKeyField = new JBPasswordField();

    private final JBTextField openRouterEndpointField = new JBTextField();
    private final JComboBox<String> openRouterModelCombo = createEditableCombo();
    private final JBPasswordField openRouterApiKeyField = new JBPasswordField();

    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
    private final JComboBox<AiSettingsState.ContextWindowPreset> contextWindowCombo =
            new JComboBox<>(AiSettingsState.ContextWindowPreset.values());
    private final JTextArea systemPromptArea = new JTextArea(5, 40);

    private JBLabel createLabel(String text) {
        JBLabel label = new JBLabel(text);
        Dimension naturalSize = label.getPreferredSize();
        label.setPreferredSize(new Dimension(JBUI.scale(120), naturalSize.height));
        return label;
    }

    private static JComboBox<String> createEditableCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setEditable(true);
        return combo;
    }

    private static String getComboText(JComboBox<String> combo) {
        Object item = combo.getEditor().getItem();
        return item != null ? item.toString() : "";
    }

    private static void setComboText(JComboBox<String> combo, String value) {
        combo.setSelectedItem(value != null ? value : "");
    }

    /**
     * 用获取到的模型列表填充下拉项,保留当前输入的文本。
     */
    private static void populateCombo(JComboBox<String> combo, java.util.List<String> models) {
        String current = getComboText(combo);
        combo.removeAllItems();
        for (String m : models) {
            combo.addItem(m);
        }
        combo.setSelectedItem(current);
    }

    /**
     * 构建 "模型下拉框 + 获取模型按钮" 的组合面板。
     */
    private JPanel buildModelPanel(JComboBox<String> modelCombo, AiSettingsState.Provider provider) {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(5), 0));
        panel.add(modelCombo, BorderLayout.CENTER);
        JButton fetchButton = new JButton("获取模型");
        fetchButton.addActionListener(e -> fetchModels(provider));
        panel.add(fetchButton, BorderLayout.EAST);
        return panel;
    }

    public AiSettingsComponent() {
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JBScrollPane scrollPane = new JBScrollPane(systemPromptArea);

        providerCards.add(buildOllamaPanel(), AiSettingsState.Provider.OLLAMA.name());
        providerCards.add(buildOpenAiPanel(), AiSettingsState.Provider.OPENAI.name());
        providerCards.add(buildOpenRouterPanel(), AiSettingsState.Provider.OPENROUTER.name());

        JPanel timeoutAndTestPanel = new JPanel(new BorderLayout());
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerPanel.add(timeoutSpinner);
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(getProvider()));
        timeoutAndTestPanel.add(spinnerPanel, BorderLayout.WEST);
        timeoutAndTestPanel.add(testButton, BorderLayout.EAST);

        JPanel contextWindowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contextWindowCombo.setSelectedItem(AiSettingsState.ContextWindowPreset.SMALL_8K);
        contextWindowPanel.add(contextWindowCombo);

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("AI Provider: "), providerCombo, 1, false)
                .addVerticalGap(5)
                .addComponent(new TitledSeparator("Provider Settings"))
                .addComponent(providerCards)
                .addLabeledComponent(createLabel("Timeout(s): "), timeoutAndTestPanel, 1, false)
                .addVerticalGap(5)
                .addComponent(new TitledSeparator("Content Limit Settings"))
                .addLabeledComponent(createLabel("\u4e0a\u4e0b\u6587\u7a97\u53e3: "), contextWindowPanel, 1, false)
                .addVerticalGap(5)
                .addComponent(new TitledSeparator("Generation Parameters"))
                .addLabeledComponent(createLabel("System Prompt: "), scrollPane, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
        providerCombo.addActionListener(e -> switchProviderCard());
    }

    private JPanel createApiKeyPanel(JBPasswordField apiKeyField) {
        apiKeyField.setColumns(30);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(apiKeyField, BorderLayout.CENTER);
        JCheckBox showPassword = new JCheckBox("Show");
        showPassword.addActionListener(e -> apiKeyField.setEchoChar(showPassword.isSelected() ? (char) 0 : '\u2022'));
        panel.add(showPassword, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildOllamaPanel() {
        JBLabel hintLabel = new JBLabel("\u63d0\u793a: \u5982\u679c URL \u4ee5 # \u7ed3\u5c3e\uff0c\u5c06\u76f4\u63a5\u4f7f\u7528\u8be5\u5730\u5740\u4f5c\u4e3a\u5b8c\u6574\u8bf7\u6c42 URL (\u4e0d\u62fc\u63a5 /api/generate)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), ollamaEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), buildModelPanel(ollamaModelCombo, AiSettingsState.Provider.OLLAMA), 1, false)
                .getPanel();
    }

    private JPanel buildOpenAiPanel() {
        JBLabel hintLabel = new JBLabel("\u63d0\u793a: \u5982\u679c URL \u4ee5 # \u7ed3\u5c3e\uff0c\u5c06\u76f4\u63a5\u4f7f\u7528\u8be5\u5730\u5740\u4f5c\u4e3a\u5b8c\u6574\u8bf7\u6c42 URL (\u4e0d\u62fc\u63a5 /v1/chat/completions)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), openAiEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), buildModelPanel(openAiModelCombo, AiSettingsState.Provider.OPENAI), 1, false)
                .addLabeledComponent(createLabel("API Key: "), createApiKeyPanel(openAiApiKeyField), 1, false)
                .getPanel();
    }

    private JPanel buildOpenRouterPanel() {
        JBLabel hintLabel = new JBLabel("\u63d0\u793a: \u5982\u679c URL \u4ee5 # \u7ed3\u5c3e\uff0c\u5c06\u76f4\u63a5\u4f7f\u7528\u8be5\u5730\u5740\u4f5c\u4e3a\u5b8c\u6574\u8bf7\u6c42 URL (\u4e0d\u62fc\u63a5 /v1/chat/completions)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), openRouterEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), buildModelPanel(openRouterModelCombo, AiSettingsState.Provider.OPENROUTER), 1, false)
                .addLabeledComponent(createLabel("API Key: "), createApiKeyPanel(openRouterApiKeyField), 1, false)
                .getPanel();
    }

    public JPanel getPanel() { return mainPanel; }

    public AiSettingsState.Provider getProvider() { return (AiSettingsState.Provider) providerCombo.getSelectedItem(); }
    public void setProvider(AiSettingsState.Provider provider) { providerCombo.setSelectedItem(provider); switchProviderCard(); }
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
    public void setOllamaEndpoint(String v) { ollamaEndpointField.setText(v != null ? v : ""); }
    public String getOllamaModel() { return getComboText(ollamaModelCombo); }
    public void setOllamaModel(String v) { setComboText(ollamaModelCombo, v); }
    public int getTimeout() { return (Integer) timeoutSpinner.getValue(); }
    public void setTimeout(int v) { timeoutSpinner.setValue(v); }
    public int getMaxDiffChars() {
        AiSettingsState.ContextWindowPreset selected = (AiSettingsState.ContextWindowPreset) contextWindowCombo.getSelectedItem();
        return selected != null ? selected.getMaxChars() : AiSettingsState.ContextWindowPreset.SMALL_8K.getMaxChars();
    }
    public void setMaxDiffChars(int v) { contextWindowCombo.setSelectedItem(AiSettingsState.ContextWindowPreset.fromMaxChars(v)); }
    public String getOpenAiEndpoint() { return openAiEndpointField.getText() != null ? openAiEndpointField.getText() : ""; }
    public void setOpenAiEndpoint(String v) { openAiEndpointField.setText(v != null ? v : ""); }
    public String getOpenAiModel() { return getComboText(openAiModelCombo); }
    public void setOpenAiModel(String v) { setComboText(openAiModelCombo, v); }
    public String getOpenAiApiKey() { return openAiApiKeyField.getPassword() != null ? String.valueOf(openAiApiKeyField.getPassword()) : ""; }
    public void setOpenAiApiKey(String v) { openAiApiKeyField.setText(v != null ? v : ""); }
    public String getSystemPrompt() { return systemPromptArea.getText() != null ? systemPromptArea.getText() : ""; }
    public void setSystemPrompt(String v) { systemPromptArea.setText(v != null ? v : ""); }
    public String getOpenRouterEndpoint() { return openRouterEndpointField.getText() != null ? openRouterEndpointField.getText() : ""; }
    public void setOpenRouterEndpoint(String v) { openRouterEndpointField.setText(v != null ? v : ""); }
    public String getOpenRouterModel() { return getComboText(openRouterModelCombo); }
    public void setOpenRouterModel(String v) { setComboText(openRouterModelCombo, v); }
    public String getOpenRouterApiKey() { return openRouterApiKeyField.getPassword() != null ? String.valueOf(openRouterApiKeyField.getPassword()) : ""; }
    public void setOpenRouterApiKey(String v) { openRouterApiKeyField.setText(v != null ? v : ""); }

    private void switchProviderCard() {
        CardLayout layout = (CardLayout) providerCards.getLayout();
        AiSettingsState.Provider provider = getProvider() != null ? getProvider() : AiSettingsState.Provider.OLLAMA;
        layout.show(providerCards, provider.name());
    }

    private void fetchModels(AiSettingsState.Provider provider) {
        String endpoint, apiKey;
        JComboBox<String> targetCombo;
        switch (provider) {
            case OLLAMA:
                endpoint = getOllamaEndpoint().trim(); apiKey = ""; targetCombo = ollamaModelCombo; break;
            case OPENAI:
                endpoint = getOpenAiEndpoint().trim(); apiKey = getOpenAiApiKey().trim(); targetCombo = openAiModelCombo; break;
            case OPENROUTER:
                endpoint = getOpenRouterEndpoint().trim(); apiKey = getOpenRouterApiKey().trim(); targetCombo = openRouterModelCombo; break;
            default:
                throw new IllegalStateException("Unknown provider: " + provider);
        }
        if (endpoint.isEmpty()) {
            Messages.showErrorDialog("Endpoint cannot be empty", "获取模型失败");
            return;
        }
        final String finalEndpoint = endpoint;
        final String finalApiKey = apiKey;
        final int timeout = getTimeout();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                java.util.List<String> models =
                        new OpenAiProviderClient().fetchModels(finalEndpoint, finalApiKey, timeout);
                SwingUtilities.invokeLater(() -> {
                    if (models.isEmpty()) {
                        Messages.showWarningDialog("未获取到任何模型", "获取模型");
                    } else {
                        populateCombo(targetCombo, models);
                        targetCombo.showPopup();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        Messages.showErrorDialog("获取模型失败: " + ex.getMessage(), "获取模型失败"));
            }
        }, "获取模型列表...", true, null);
    }

    private void testConnection(AiSettingsState.Provider provider) {
        String endpoint, model, apiKey;
        switch (provider) {
            case OLLAMA: endpoint = getOllamaEndpoint().trim(); model = getOllamaModel().trim(); apiKey = ""; break;
            case OPENAI: endpoint = getOpenAiEndpoint().trim(); model = getOpenAiModel().trim(); apiKey = getOpenAiApiKey().trim(); break;
            case OPENROUTER: endpoint = getOpenRouterEndpoint().trim(); model = getOpenRouterModel().trim(); apiKey = getOpenRouterApiKey().trim(); break;
            default: throw new IllegalStateException("Unknown provider: " + provider);
        }
        if (endpoint.isEmpty()) { Messages.showErrorDialog("Endpoint cannot be empty", "Test Connection Failed"); return; }
        if (model.isEmpty()) { Messages.showErrorDialog("Model cannot be empty", "Test Connection Failed"); return; }
        if ((provider == AiSettingsState.Provider.OPENAI || provider == AiSettingsState.Provider.OPENROUTER) && apiKey.isEmpty()) {
            Messages.showErrorDialog("API Key cannot be empty", "Test Connection Failed"); return;
        }
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                GenerationInputs inputs = new GenerationInputs("Test connection",
                        "You are a test assistant. Reply with 'OK' if you receive this message.",
                        endpoint, model, apiKey, getTimeout());
                String response;
                switch (provider) {
                    case OLLAMA: response = new OllamaProviderClient().generate(inputs, null); break;
                    case OPENAI: response = new OpenAiProviderClient().generate(inputs, null); break;
                    case OPENROUTER: response = new OpenRouterProviderClient().generate(inputs, null); break;
                    default: throw new IllegalStateException("Unknown provider: " + provider);
                }
                String finalResponse = response;
                SwingUtilities.invokeLater(() -> Messages.showInfoMessage(
                        "Connection successful!\n\nProvider: " + provider + "\nModel: " + model +
                                "\nResponse: " + finalResponse.substring(0, Math.min(100, finalResponse.length())) + "...",
                        "Test Connection Successful"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Connection failed: " + ex.getMessage(), "Test Connection Failed"));
            }
        }, "Testing Connection...", true, null);
    }
}
