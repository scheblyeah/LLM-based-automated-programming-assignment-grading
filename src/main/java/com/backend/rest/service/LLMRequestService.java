package com.backend.rest.service;

import com.backend.rest.config.PromptConfig;
import com.backend.rest.config.PromptEnhancementConfig;
import com.backend.rest.dto.*;
import com.backend.rest.enums.PromptingStyle;
import com.backend.rest.enums.RequestOrchestration;
import com.backend.rest.extractor.GeneratedTestsResponseExtractor;
import com.backend.rest.extractor.GradingResponseExtractor;
import com.backend.rest.enums.UsedLLM;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMRequestService {
    private int TOKENS_PER_REQUEST = 4000;


    //todo aus model und token ein dto machen und das bei jedem request zurückgeben
    //
    // + und wenn in funktionen member variablen gesetzt werden, rückgabeDTO machen und geben

    private final PromptConfig promptConfig;


    @Autowired
    public LLMRequestService(PromptConfig promptConfig) {
        this.promptConfig = promptConfig;
    }

    public String putVariablesInPrompt(String promptTemplateStr, SubmissionDTOSingle submissionDTO, String gradingScheme) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("description", submissionDTO.taskDescription);
        variables.put("submission", submissionDTO.studentSubmission);
        variables.put("gradingScheme", gradingScheme);
        variables.put("sampleSolution", submissionDTO.sampleSolution);
        variables.put("testCases", submissionDTO.humanGeneratedTestCases);

        PromptTemplate promptTemplate = new PromptTemplate(promptTemplateStr);
        Prompt userPrompt = promptTemplate.apply(variables);
        return userPrompt.text();
    }

    public GradingResponse getParsedResponse(String prompt, LLMRequestConfiguration config) {
        GradingResponseExtractor gradingResponseExtractor = AiServices.create(GradingResponseExtractor.class, setChatLanguageModel(config));
        GradingResponse gradingResponse = gradingResponseExtractor.extractGradingResponseFrom(prompt);
        gradingResponse.calcTotalAchievedAndTotalMaxPoints();
        return gradingResponse;
    }

    // New method to handle enhanced requests
    public GradingResponse getGradingResponseWithEnhancements(SubmissionDTOSingle submissionDTO, String gradingScheme,
                                                              PromptEnhancementConfig enhancementConfig, LLMRequestConfiguration mainConfig) {
        String basePrompt = selectBasePrompt(enhancementConfig.getPromptingStyle());
        String initialPrompt = putVariablesInPrompt(basePrompt, submissionDTO, gradingScheme);

        if (enhancementConfig.getRequestOrchestration() == RequestOrchestration.ITERATIVE_PROMPTING) {
            return handleIterativePrompting(submissionDTO, gradingScheme, mainConfig);
        }

        GradingResponse initialResponse = getParsedResponse(initialPrompt, mainConfig);

        switch (enhancementConfig.getRequestOrchestration()) {
            case OUTPUT_VERIFICATION_SAME_MODEL:
                return verifyResponse(initialResponse, submissionDTO, gradingScheme, mainConfig);
            case OUTPUT_VERIFICATION_DIFFERENT_MODEL:
                LLMRequestConfiguration verificationConfig = new LLMRequestConfiguration(
                        enhancementConfig.getVerificationModel(), mainConfig.apiToken());
                return verifyResponse(initialResponse, submissionDTO, gradingScheme, verificationConfig);
            case NONE:
            default:
                return initialResponse;
        }
    }

    private String selectBasePrompt(PromptingStyle style) {
        switch (style) {
            case ONE_SHOT:
                return promptConfig.getOneShot();
            case FEW_SHOT:
                return promptConfig.getFewShot();
            case CHAIN_OF_THOUGHT:
                return promptConfig.getChainOfThought();
            case NORMAL:
            default:
                return promptConfig.getUserGradingResponse();
        }
    }

    private GradingResponse handleIterativePrompting(SubmissionDTOSingle submissionDTO, String gradingScheme, LLMRequestConfiguration config) {
        List<Subtask> subtasks = parseGradingScheme(gradingScheme);
        List<GradingSchemeSubtask> gradedSubtasks = new ArrayList<>();

        for (Subtask subtask : subtasks) {
            String subtaskPrompt = createSubtaskPrompt(subtask, submissionDTO);
            String response = makeChatRequestWithString("", subtaskPrompt, true, config);
            GradingSchemeSubtask gradedSubtask = parseSubtaskResponse(response, subtask);
            gradedSubtasks.add(gradedSubtask);
        }

        GradingResponse gradingResponse = new GradingResponse();
        gradingResponse.setGradingSchemeSubtasks(gradedSubtasks);
        return gradingResponse;
    }

    private List<Subtask> parseGradingScheme(String gradingScheme) {
        List<Subtask> subtasks = new ArrayList<>();
        String[] lines = gradingScheme.split("\n");
        for (String line : lines) {
            String[] parts = line.split("\\(");
            if (parts.length == 2) {
                String description = parts[0].trim();
                String pointsStr = parts[1].replace(" points)", "").trim();
                int maxPoints = Integer.parseInt(pointsStr);
                subtasks.add(new Subtask(description, maxPoints));
            }
        }
        return subtasks;
    }

    private String createSubtaskPrompt(Subtask subtask, SubmissionDTOSingle submissionDTO) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("subtaskDescription", subtask.description());
        variables.put("maxPoints", subtask.maxPoints());
        variables.put("description", submissionDTO.taskDescription);
        variables.put("submission", submissionDTO.studentSubmission);

        PromptTemplate template = new PromptTemplate(
                "Grade this submission for the subtask: {{subtaskDescription}} (max {{maxPoints}} points). " +
                        "Task description: {{description}}. Submission: {{submission}}. " +
                        "Return a JSON object with 'gradingPoints' (0 to {{maxPoints}}) and 'justification'."
        );
        return template.apply(variables).text();
    }

    private GradingSchemeSubtask parseSubtaskResponse(String response, Subtask subtask) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> responseMap = mapper.readValue(response, Map.class);
            int gradingPoints = (int) responseMap.get("gradingPoints");
            String justification = (String) responseMap.get("justification");
            return new GradingSchemeSubtask(subtask.description(), gradingPoints, subtask.maxPoints(), justification);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse subtask response", e);
        }
    }

    private GradingResponse verifyResponse(GradingResponse initialResponse, SubmissionDTOSingle submissionDTO,
                                           String gradingScheme, LLMRequestConfiguration config) {
        String verificationPrompt = createVerificationPrompt(initialResponse, submissionDTO, gradingScheme);
        return getParsedResponse(verificationPrompt, config);
    }

    private String createVerificationPrompt(GradingResponse initialResponse, SubmissionDTOSingle submissionDTO, String gradingScheme) {
        ObjectMapper mapper = new ObjectMapper();
        String initialResponseStr;
        try {
            initialResponseStr = mapper.writeValueAsString(initialResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize initial response", e);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("initialResponse", initialResponseStr);
        variables.put("gradingScheme", gradingScheme);
        variables.put("description", submissionDTO.taskDescription);

        PromptTemplate template = new PromptTemplate(
                "Here is a grading response: {{initialResponse}}. Verify its accuracy and fairness based on " +
                        "the grading scheme: {{gradingScheme}} and task description: {{description}}. " +
                        "Provide a refined grading in the same format if necessary."
        );
        return template.apply(variables).text();
    }

    public GeneratedTestResults getParsedResponseGenerateTests(String prompt, LLMRequestConfiguration config) {
        GeneratedTestsResponseExtractor gradingResponseExtractor = AiServices.create(GeneratedTestsResponseExtractor.class, setChatLanguageModel(config));
        GeneratedTestResults gradingResponse = gradingResponseExtractor.extractGeneratedTestsResponseFrom(prompt);

        System.out.println(gradingResponse);

        return gradingResponse;
    }

    private ChatLanguageModel setChatLanguageModel(LLMRequestConfiguration config){
        ChatLanguageModel chatLanguageModel;
        if (config.usedModel() == UsedLLM.GEMINI_FLASH || config.usedModel() == UsedLLM.GEMINI_PRO) {
            chatLanguageModel = GoogleAiGeminiChatModel.builder()
                    .apiKey(config.apiToken())
                    .modelName(config.usedModel().getModelName())
                    .responseFormat(ResponseFormat.JSON)
                    .temperature(0.0)
                    .build();
        } else {
            chatLanguageModel = OpenAiChatModel.builder()
                    .apiKey(config.apiToken())
                    .modelName(config.usedModel().getModelName())
                    .responseFormat("json_schema")
                    .strictJsonSchema(true)
                    .temperature(1D)
                    .timeout(Duration.ofMinutes(15))
                    .build();
        }
        return chatLanguageModel;
    }


    public String makeChatRequest(List<ChatMessage> messages, boolean jsonMode, LLMRequestConfiguration config) {
        System.out.println("Making LLM Request with Model: " + config.usedModel().getModelName());
        System.out.println(config.apiToken());
        ChatLanguageModel model;

        if (config.usedModel() == UsedLLM.GEMINI_FLASH || config.usedModel() == UsedLLM.GEMINI_PRO) {
            var builder = GoogleAiGeminiChatModel.builder()
                    .apiKey(config.apiToken())
                    .modelName(config.usedModel().getModelName());

            if (jsonMode) {
                builder.responseFormat(ResponseFormat.JSON);
            } else {
                builder.responseFormat(ResponseFormat.TEXT);
            }
            model = builder.build();
        } else {
            var builder = OpenAiChatModel.builder()
                    .apiKey(config.apiToken())
                    .modelName(config.usedModel().getModelName())
                    .maxCompletionTokens(TOKENS_PER_REQUEST);
            // chatGPT complains that requests for grading scheme doesn't return JSON format
            if (jsonMode) {
                builder.responseFormat("json_object");
            }
            model = builder.build();
        }

        Response<AiMessage> response = model.generate(messages);
        System.out.println("Prompt:" + messages.toString());
        System.out.println("Total tokens used: " + response.tokenUsage().totalTokenCount());
        System.out.printf("Total Cost of Call & Response: %.10f$\n", config.usedModel().getTotalCost(response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount()));
        System.out.println("-------------------------------------------------------------------------------");
        //this.llmRequestCost = "Total tokens used: " + response.tokenUsage().totalTokenCount() + ". Total Cost of Call and Response: $" + String.format("%.4f", usedModel.getTotalCost(response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount()));
        return response.content().text();
    }


    public String makeChatRequestWithString(String promptSystem, String promptUser, boolean jsonMode, LLMRequestConfiguration config) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new SystemMessage(promptSystem);
        ChatMessage userMessage = new UserMessage(promptUser);
        messages.add(chatMessage);
        messages.add(userMessage);
        return makeChatRequest(messages, jsonMode, config);
    }

    public String generateGradingSchemeIfNotGiven(String gradingSchemeFromFrontend, String taskDescription, LLMRequestConfiguration config) {
        if (gradingSchemeFromFrontend != null && !gradingSchemeFromFrontend.equals("")) {
            return gradingSchemeFromFrontend;
        } else {
            //let LLM generate grading scheme...
            String gradingSchemePrompt = promptConfig.getGradingSchemeToBeGenerated();
            return makeChatRequestWithString(gradingSchemePrompt, promptConfig.getDescription() + taskDescription, false, config);
        }
    }
}
