package com.backend.rest.config;

import com.backend.rest.enums.PromptingStyle;
import com.backend.rest.enums.RequestOrchestration;
import com.backend.rest.enums.UsedLLM;

public class PromptEnhancementConfig {
    private PromptingStyle promptingStyle;
    private RequestOrchestration requestOrchestration;
    private UsedLLM verificationModel; // Optional, for different model verification

    public PromptEnhancementConfig(PromptingStyle promptingStyle, RequestOrchestration requestOrchestration) {
        this.promptingStyle = promptingStyle;
        this.requestOrchestration = requestOrchestration;
    }

    public PromptEnhancementConfig(PromptingStyle promptingStyle, RequestOrchestration requestOrchestration, UsedLLM verificationModel) {
        this.promptingStyle = promptingStyle;
        this.requestOrchestration = requestOrchestration;
        this.verificationModel = verificationModel;
    }

    public PromptingStyle getPromptingStyle() {
        return promptingStyle;
    }

    public RequestOrchestration getRequestOrchestration() {
        return requestOrchestration;
    }

    public UsedLLM getVerificationModel() {
        return verificationModel;
    }
}