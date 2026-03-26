package com.github.senx.aicommit.service.provider;

import com.github.senx.aicommit.service.model.GenerationInputs;
import com.github.senx.aicommit.service.util.CommitMessageCleaner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.*;

import java.io.IOException;

public class OpenAiProviderClient extends BaseHttpProviderClient {

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
                .build();

        Call call = client.newCall(request);
        this.ongoingCall = call;

        try {
            checkCanceled(indicator);
            try (Response response = call.execute()) {
                checkCanceled(indicator);

                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String errorDetail = "\u672a\u77e5\u9519\u8bef";
                    String suggestion = "";
                    try {
                        JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                        if (errorJson.has("error")) {
                            JsonElement errorElement = errorJson.get("error");
                            if (errorElement.isJsonObject()) {
                                JsonObject errorObj = errorElement.getAsJsonObject();
                                String msg = errorObj.has("message") ? errorObj.get("message").getAsString() : "";
                                String code = errorObj.has("code") && !errorObj.get("code").isJsonNull() ? errorObj.get("code").getAsString() : "";
                                StringBuilder sb = new StringBuilder();
                                if (!msg.isEmpty()) sb.append(msg);
                                if (!code.isEmpty()) sb.append(" (Code: ").append(code).append(")");
                                errorDetail = sb.length() > 0 ? sb.toString() : errorElement.toString();
                            } else if (errorElement.isJsonPrimitive()) {
                                errorDetail = errorElement.getAsString();
                            }
                            if (errorDetail.contains("exceed") || errorDetail.contains("token")
                                    || errorDetail.contains("length") || errorDetail.contains("maximum")) {
                                suggestion = "\n\n\ud83d\udca1 \u5efa\u8bae\uff1a\u8bf7\u5728\u8bbe\u7f6e\u4e2d\u9009\u62e9\u66f4\u5c0f\u7684\"\u4e0a\u4e0b\u6587\u7a97\u53e3\"\u9884\u8bbe\uff0c\u6216\u51cf\u5c11\u63d0\u4ea4\u7684\u6587\u4ef6\u6570\u91cf\u3002";
                            }
                        }
                    } catch (Exception ignored) {
                        errorDetail = responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
                    }
                    throw new IOException("API \u9519\u8bef (" + response.code() + "): " + errorDetail + suggestion);
                }
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (!jsonResponse.has("choices") || !jsonResponse.get("choices").isJsonArray()
                        || jsonResponse.getAsJsonArray("choices").isEmpty()) {
                    throw new IOException("Invalid response format from OpenAI");
                }

                JsonObject firstChoice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                JsonObject message = firstChoice.has("message") ? firstChoice.getAsJsonObject("message") : null;

                String rawResponse = (message != null && message.has("content"))
                        ? message.get("content").getAsString().trim()
                        : null;

                if (rawResponse == null) {
                    throw new IOException("Invalid response format from OpenAI: missing content");
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
