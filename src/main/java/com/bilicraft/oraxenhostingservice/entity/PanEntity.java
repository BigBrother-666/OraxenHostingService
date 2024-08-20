package com.bilicraft.oraxenhostingservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PanEntity {
    public int code;
    public String message;
    public Map<String, Object> data;
    @JsonProperty("x-traceID")
    public String xTraceID;
}
