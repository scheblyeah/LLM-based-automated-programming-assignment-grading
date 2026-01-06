package com.backend.rest.dto;

import com.backend.rest.enums.UsedLLM;

public record LLMRequestConfiguration(UsedLLM usedModel, String apiToken) {

}
