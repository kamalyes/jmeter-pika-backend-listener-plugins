package io.kamalyes.jmeter.pika.client;

import org.apache.jmeter.samplers.SampleResult;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.kamalyes.jmeter.entity.SendReqData;
import io.kamalyes.jmeter.entity.TestCaseInfo;
import io.kamalyes.jmeter.entity.TestSummary;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class JmeterPikaBackendListenerClient extends AbstractBackendListenerClient {

    private static final Logger logger = LoggerFactory.getLogger(JmeterPikaBackendListenerClient.class);

    private static String pikaServerUrl;

    private static String projectName;

    private static String envName;
    private static String batchNo;

    private TestSummary testSummary;

    private ArrayList<TestCaseInfo> testCases;

    private Integer countSuccess;

    private List<String> labelPrefix;

    private Integer currentDeep = 0;

    @Override
    public void setupTest(BackendListenerContext context) {
        logger.info(" ---- Test Start ---- ");
        JmeterPikaBackendListenerClient.pikaServerUrl = context.getParameter("pikaServerUrl");
        JmeterPikaBackendListenerClient.projectName = context.getParameter("projectName");
        JmeterPikaBackendListenerClient.envName = context.getParameter("envName");
        JmeterPikaBackendListenerClient.batchNo = context.getParameter("batchNo");

        this.labelPrefix = new ArrayList<>();
        this.currentDeep = 0;
        this.testCases = new ArrayList<>();
        this.testSummary = new TestSummary();
        this.countSuccess = 0;

        this.testSummary.setProject(JmeterPikaBackendListenerClient.projectName);
        this.testSummary.setEnv(JmeterPikaBackendListenerClient.envName);
        this.testSummary.setBatchNo(JmeterPikaBackendListenerClient.batchNo);

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") || os.contains("mac")) {
            // 手动构建
            this.testSummary.setOsType(1);
        } else {
            // 自动构建
            this.testSummary.setOsType(2);
        }
        this.testSummary.setStartTime(System.currentTimeMillis());
    }

    @Override
    public void teardownTest(BackendListenerContext context) {
        this.testSummary.setEndTime(System.currentTimeMillis());
        this.testSummary.setDuration((this.testSummary.getEndTime() - this.testSummary.getStartTime()) / 1000);
        SendReqData sendReqData = new SendReqData();
        sendReqData.setTestSummary(this.testSummary);
        sendReqData.setTestCases(this.testCases);
        // 发送
        this.sendHttp(sendReqData);
        logger.info(" ---- Test End ---- ");
    }

    public void handlerResult(SampleResult sampleResult) {
        logger.info("当前请求：".concat(sampleResult.getSampleLabel()));
        Class<? extends SampleResult> aClass = sampleResult.getClass();
        if (!aClass.getName().contains("http.sampler")) {
            logger.info("非http请求：".concat(sampleResult.getSampleLabel()));
            this.labelPrefix.add(sampleResult.getSampleLabel());
            SampleResult[] subResults = sampleResult.getSubResults();
            if (subResults.length != 0) {
                for (SampleResult result : subResults) {
                    this.currentDeep += 1;
                    handlerResult(result);
                    this.currentDeep -= 1;
                }
            } else {
                logger.info("非事务控制器：".concat(sampleResult.getSampleLabel()));
            }

        } else {
            logger.info("LabelPrefix: ".concat(this.labelPrefix.toString()));
            TestCaseInfo testCaseInfo = new TestCaseInfo();
            HTTPSampleResult httpSampleResult = (HTTPSampleResult) sampleResult;
            if (!this.labelPrefix.isEmpty()) {
                String prefix = String.join("-", this.labelPrefix.subList(0, this.currentDeep));
                testCaseInfo.setCaseName(prefix.concat("-").concat(httpSampleResult.getSampleLabel()));
            } else {
                testCaseInfo.setCaseName(httpSampleResult.getSampleLabel());
            }

            testCaseInfo.setModuleName(httpSampleResult.getThreadName().split(" ")[0]);
            testCaseInfo.setStartTime(httpSampleResult.getStartTime());
            testCaseInfo.setEndTime(httpSampleResult.getEndTime());
            testCaseInfo.setRequestUrl(httpSampleResult.getUrlAsString());
            testCaseInfo.setRequestHeader(httpSampleResult.getRequestHeaders());
            testCaseInfo.setRequestBody(httpSampleResult.getSamplerData());
            testCaseInfo.setRequestMethod(httpSampleResult.getHTTPMethod());

            testCaseInfo.setResponseCode(httpSampleResult.getResponseCode());
            testCaseInfo.setResponseHeader(httpSampleResult.getResponseHeaders());
            testCaseInfo.setResponseBody(httpSampleResult.getResponseDataAsString());

            testCaseInfo.setTestResult(true);
            if (!(testCaseInfo.getResponseCode().startsWith("2") || testCaseInfo.getResponseCode().startsWith("3"))) {
                testCaseInfo.setTestResult(false);
            }
            AssertionResult[] assertionResults = httpSampleResult.getAssertionResults();
            StringBuilder sb = new StringBuilder();
            for (AssertionResult assertionResult : assertionResults) {
                if (assertionResult.isFailure()) {
                    testCaseInfo.setTestResult(false);
                    sb.append(assertionResult.getName()).append(": ").append(assertionResult.getFailureMessage())
                            .append("\n");
                }
            }
            if (Boolean.TRUE.equals(testCaseInfo.getTestResult())) {
                testCaseInfo.setTestResult(true);
                this.countSuccess += 1;
            }
            testCaseInfo.setFailMessage(sb.toString());
            this.testCases.add(testCaseInfo);
        }
    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext backendListenerContext) {
        for (SampleResult sampleResult : sampleResults) {
            handlerResult(sampleResult);
            this.labelPrefix.clear();
        }

        this.testSummary.setTotal(this.testCases.size());
        this.testSummary.setSuccess(this.countSuccess);
        this.testSummary.setFailure(this.testCases.size() - this.countSuccess);
        if (this.testSummary.getTotal() == 0) {
            this.testSummary.setPassRate((double) 0);
        } else {
            double i = (double) this.testSummary.getSuccess() / this.testSummary.getTotal();
            BigDecimal bd = BigDecimal.valueOf(i);
            double rate = bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
            this.testSummary.setPassRate(rate);
        }
        this.testSummary.setResult(this.testSummary.getFailure() == 0);
    }

    private void sendHttp(SendReqData sendReqData) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setPrettyPrinting().create();
        String prettyJson = gson.toJson(sendReqData);
        logger.info("SendReqData===========》\n".concat(prettyJson));
        HttpResponse<JsonNode> response = null;
        String message = "数据推送给Pika";
        try {
            response = Unirest.post(JmeterPikaBackendListenerClient.pikaServerUrl)
                    .header("Content-Type", "application/json")
                    .body(prettyJson)
                    .asJson();
            logger.info(message.concat("成功：").concat(response.getBody().toString()));
        } catch (UnirestException e) {
            logger.error(message.concat("异常：").concat(e.getMessage()));
        }
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument("pikaServerUrl", "http://localhost:7777/jmeter/upload_result");
        arguments.addArgument("projectName", "项目名称${__timeShift(,,,,)}");
        arguments.addArgument("envName", "环境名称${__timeShift(,,,,)}");
        arguments.addArgument("batchNo", "用例批次编号${__timeShift(,,,,)}");
        return arguments;
    }
}