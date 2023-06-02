package io.kamalyes.jmeter.entity;

import lombok.Data;

@Data
public class TestSummary {
    private String project;
    private String env;
    private String batchNo;
    private Integer osType;
    private Integer total;
    private Integer success;
    private Integer failure;
    private Double passRate;
    private Boolean result;
    private Long startTime;
    private Long endTime;
    private Long duration;
}
