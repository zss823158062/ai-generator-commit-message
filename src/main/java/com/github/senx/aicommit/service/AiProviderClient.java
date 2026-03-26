package com.github.senx.aicommit.service;

import com.github.senx.aicommit.service.model.GenerationInputs;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface AiProviderClient {

    String generate(GenerationInputs inputs, @Nullable ProgressIndicator indicator) throws IOException;

    void cancel();
}
