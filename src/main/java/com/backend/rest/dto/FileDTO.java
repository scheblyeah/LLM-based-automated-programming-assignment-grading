package com.backend.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileDTO {
    @JsonProperty("fileName")
    public String fileName;

    @JsonProperty("fileContent")
    public String fileContent;
}
