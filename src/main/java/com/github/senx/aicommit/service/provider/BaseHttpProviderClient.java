package com.github.senx.aicommit.service.provider;

import com.github.senx.aicommit.service.AiProviderClient;
import com.github.senx.aicommit.service.model.GenerationInputs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class BaseHttpProviderClient implements AiProviderClient {

    protected static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    protected final Gson gson = new Gson();
    protected volatile Call ongoingCall;

    @Override
    public void cancel() {
        Call call = this.ongoingCall;
        if (call != null) {
            call.cancel();
        }
    }

    protected OkHttpClient buildClient(GenerationInputs inputs) {
        return new OkHttpClient.Builder()
                .connectTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    protected void checkCanceled(@Nullable ProgressIndicator indicator) {
        if (indicator != null && indicator.isCanceled()) {
            throw new ProcessCanceledException();
        }
    }

    protected String normalizeBaseUrl(String base) {
        if (base == null || base.isEmpty()) {
            return "";
        }
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    /**
     * 获取可用模型列表。请求 OpenAI 兼容的 /v1/models 端点(Ollama 亦支持)。
     * endpoint 以 # 结尾时,直接使用去掉 # 后的地址作为完整请求 URL。
     *
     * @param endpoint       服务基础地址,或以 # 结尾的完整地址
     * @param apiKey         API Key,为空则不发送 Authorization 头
     * @param timeoutSeconds 超时时间(秒)
     * @return 模型 id 列表
     */
    public List<String> fetchModels(String endpoint, String apiKey, int timeoutSeconds) throws IOException {
        String base = endpoint != null ? endpoint.trim() : "";
        if (base.isEmpty()) {
            throw new IOException("Endpoint cannot be empty");
        }
        String url = base.endsWith("#")
                ? base.substring(0, base.length() - 1)
                : normalizeBaseUrl(base) + "/v1/models";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        Request.Builder builder = new Request.Builder().url(url).get();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiKey.trim());
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                String detail = responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
                throw new IOException("获取模型失败 (" + response.code() + "): " + detail);
            }
            return parseModels(responseBody);
        }
    }

    private List<String> parseModels(String responseBody) throws IOException {
        List<String> models = new ArrayList<>();
        JsonObject json;
        try {
            json = gson.fromJson(responseBody, JsonObject.class);
        } catch (Exception e) {
            throw new IOException("无法解析模型列表响应");
        }
        if (json == null) {
            throw new IOException("模型列表响应为空");
        }

        // OpenAI 兼容格式: { "data": [ { "id": "..." } ] }
        JsonArray items = null;
        String idKey = null;
        if (json.has("data") && json.get("data").isJsonArray()) {
            items = json.getAsJsonArray("data");
            idKey = "id";
        } else if (json.has("models") && json.get("models").isJsonArray()) {
            // Ollama /api/tags 格式: { "models": [ { "name": "..." } ] }
            items = json.getAsJsonArray("models");
            idKey = "name";
        }

        if (items == null) {
            throw new IOException("响应中未找到模型列表");
        }

        for (JsonElement el : items) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String id = null;
            if (obj.has(idKey) && !obj.get(idKey).isJsonNull()) {
                id = obj.get(idKey).getAsString();
            } else if (obj.has("id") && !obj.get("id").isJsonNull()) {
                id = obj.get("id").getAsString();
            } else if (obj.has("name") && !obj.get("name").isJsonNull()) {
                id = obj.get("name").getAsString();
            }
            if (id != null && !id.trim().isEmpty()) {
                models.add(id.trim());
            }
        }
        return models;
    }
}
