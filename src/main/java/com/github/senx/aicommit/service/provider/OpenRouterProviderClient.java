package com.github.senx.aicommit.service.provider;

import com.github.senx.aicommit.service.model.GenerationInputs;
import com.github.senx.aicommit.service.util.CommitMessageCleaner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.*;

import java.io.IOException;

public class OpenRouterProviderClient extends BaseHttpProviderClient {

    @Override
    public String generate(GenerationInputs inputs, ProgressIndicator indicator) throws IOException {
        OkHttpClient client = buildClient(inputs);

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", inputs.systemPrompt);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", inputs.prompt);

        JsonArray messages = new JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", inputs.model);
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);

        String url = inputs.endpoint.endsWith("#")
                ? inputs.endpoint.substring(0, inputs.endpoint.length() - 1)
                : normalizeBaseUrl(inputs.endpoint) + "/v1/chat/completions";
        String jsonBody = gson.toJson(requestBody);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + inputs.apiKey)
                .addHeader("HTTP-Referer", "https://github.com/zss823158062/ai-generator-commit-message")
                .addHeader("X-Title", "AI Commit Message Generator")
                .build();

        Call call = client.newCall(request);
        this.ongoingCall = call;

        try {
            checkCanceled(indicator);
            try (Response response = call.execute()) {
                checkCanceled(indicator);

                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response);
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (!jsonResponse.has("choices") || !jsonResponse.get("choices").isJsonArray()
                        || jsonResponse.getAsJsonArray("choices").isEmpty()) {
                    throw new IOException("Invalid response format from OpenRouter");
                }

                JsonObject firstChoice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                JsonObject message = firstChoice.has("message") ? firstChoice.getAsJsonObject("message") : null;

                String rawResponse = (message != null && message.has("content"))
                        ? message.get("content").getAsString().trim()
                        : null;

                if (rawResponse == null) {
                    throw new IOException("Invalid response format from OpenRouter: missing content");
                }

                return CommitMessageCleaner.clean(rawResponse);
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (IOException e) {
            if (indicator != null && indicator.isCanceled()) {
                throw new ProcessCanceledException();
            }
            throw e;
        } finally {
            this.ongoingCall = null;
        }
    }
}
