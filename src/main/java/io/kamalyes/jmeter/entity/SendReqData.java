package io.kamalyes.jmeter.entity;

import java.util.ArrayList;

import lombok.Data;

@Data
public class SendReqData {
    private TestSummary testSummary;
    private ArrayList<TestCaseInfo> testCases;
}
