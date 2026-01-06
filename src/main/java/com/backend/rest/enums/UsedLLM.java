package com.backend.rest.enums;

public enum UsedLLM {
    //GPT_4o_MINI("gpt-4o-mini", 0.3, 1.2),
    GPT_4_1_NANO("gpt-4.1-nano", 0.2, 0.8),
    GPT_5_NANO("gpt-5-nano", 0.05, 0.4),
    GPT_5_MINI("gpt-5-mini", 0.25, 2),
    GPT_5("gpt-5", 1.25, 10),

    //GPT_4o("gpt-4o", 5, 15),
    GEMINI_FLASH("gemini-1.5-flash", 0.075, 0.3),
    GEMINI_PRO("gemini-1.5-pro", 1.25, 5);
    private final String modelName;
    private final double pricePerMillionInputToken; // in US-$
    private final double pricePerMillionOutputToken;

    UsedLLM(String modelName, double pricePerInputToken, double pricePerOutputToken) {
        this.modelName = modelName;
        this.pricePerMillionInputToken = pricePerInputToken;
        this.pricePerMillionOutputToken = pricePerOutputToken;
    }

    public String getModelName() {
        return modelName;
    }

    public double getPricePerMillionInputToken() {
        return pricePerMillionInputToken;
    }

    public double getPricePerMillionOutputToken() {
        return pricePerMillionOutputToken;
    }

    public double getTotalCost(int inputTokens, int outputTokens){
        return inputTokens * (pricePerMillionInputToken/1000000) + outputTokens* (pricePerMillionOutputToken/1000000);
    }
}
