package io.kamalyes.dtos;

import java.util.ArrayList;

import lombok.Data;

@Data
public class SendReqDataDTO {
    private TestSummaryDTO testSummary;
    private ArrayList<TestCaseInfoDTO> testCases;
}
