package com.github.jdami.aicommit.service.provider;

import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.github.jdami.aicommit.service.util.CommitMessageCleaner;
import com.google.gson.JsonObject;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.*;

import java.io.IOException;

public class OllamaProviderClient extends BaseHttpProviderClient {

    @Override
    public String generate(GenerationInputs inputs, ProgressIndicator indicator) throws IOException {
        OkHttpClient client = buildClient(inputs);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", inputs.model);
        requestBody.addProperty("prompt", inputs.prompt);
        requestBody.addProperty("system", inputs.systemPrompt);
        requestBody.addProperty("stream", false);

        String jsonBody = gson.toJson(requestBody);
        String url = normalizeBaseUrl(inputs.endpoint) + "/api/generate";

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder().url(url).post(body).build();

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

                if (!jsonResponse.has("response")) {
                    throw new IOException("Invalid response format from Ollama");
                }

                String rawResponse = jsonResponse.get("response").getAsString().trim();
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
