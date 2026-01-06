package com.backend.rest.dto;

import com.backend.rest.enums.PromptingStyle;
import com.backend.rest.enums.RequestOrchestration;
import com.backend.rest.enums.UsedLLM;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmissionDTOMultiple {
    public PromptingStyle promptingStyle = PromptingStyle.FEW_SHOT;
    public RequestOrchestration requestOrchestration = RequestOrchestration.NONE;
    public String taskDescription;
    @JsonProperty("fileDTOs")
    public FileDTO[] fileDTOs;

    public String humanGeneratedTestCases;
    public String sampleSolution;
    public String gradingScheme;

    public String selectedModel;
    public String apiToken;
    public Boolean generateGradingBool;
    public Boolean generateExecuteTestsBool;
    public UsedLLM verificationModel;
}