package io.kamalyes.jmeter.entity;

import lombok.Data;

@Data
public class TestCaseInfo {
    private String moduleName;
    private String caseName;
    private Long startTime;
    private Long endTime;
    private String requestUrl;
    private String requestMethod;
    private String requestHeader;
    private String requestBody;
    private String responseCode;
    private String responseHeader;
    private String responseBody;
    private Boolean testResult;
    private String failMessage;
}
