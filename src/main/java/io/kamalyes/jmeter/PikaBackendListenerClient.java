package io.kamalyes.jmeter;

import org.apache.jmeter.samplers.SampleResult;

import com.alibaba.fastjson.JSON;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.kamalyes.constants.HttpMethodConstants;
import io.kamalyes.dtos.ErrorReportAssertionResultDTO;
import io.kamalyes.dtos.ResponseAssertionResultDTO;
import io.kamalyes.dtos.SendReqDataDTO;
import io.kamalyes.dtos.TestCaseInfoDTO;
import io.kamalyes.dtos.TestSummaryDTO;
import io.kamalyes.utils.LoggerUtil;
import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Data
public class PikaBackendListenerClient extends AbstractBackendListenerClient {
    private static String pikaServerUrl;

    private static String projectName;

    private static String envName;
    private static String batchNo;

    private TestSummaryDTO testSummary;

    private ArrayList<TestCaseInfoDTO> testCases;

    private Integer countSuccess;

    private List<String> labelPrefix;

    private Integer currentDeep = 0;

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        PikaBackendListenerClient.pikaServerUrl = context.getParameter("pikaServerUrl");
        PikaBackendListenerClient.projectName = context.getParameter("projectName");
        PikaBackendListenerClient.envName = context.getParameter("envName");
        PikaBackendListenerClient.batchNo = context.getParameter("batchNo");

        this.labelPrefix = new ArrayList<>();
        this.currentDeep = 0;
        this.testCases = new ArrayList<>();
        this.testSummary = new TestSummaryDTO();
        this.countSuccess = 0;

        this.testSummary.setProject(PikaBackendListenerClient.projectName);
        this.testSummary.setEnv(PikaBackendListenerClient.envName);
        this.testSummary.setBatchNo(PikaBackendListenerClient.batchNo);

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") || os.contains("mac")) {
            // 手动构建
            this.testSummary.setOsType(1);
        } else {
            // 自动构建
            this.testSummary.setOsType(2);
        }
        this.testSummary.setStartTime(System.currentTimeMillis());
        LoggerUtil.info("TestStarted接收到参数：报告【" + JSON.toJSONString(testSummary) + " 】");
        super.setupTest(context);
    }

    @Override
    public void teardownTest(BackendListenerContext context) {
        this.testSummary.setEndTime(System.currentTimeMillis());
        this.testSummary.setDuration((this.testSummary.getEndTime() - this.testSummary.getStartTime()) / 1000);
        SendReqDataDTO sendReqData = new SendReqDataDTO();
        sendReqData.setTestSummary(this.testSummary);
        sendReqData.setTestCases(this.testCases);
        // 发送
        this.sendHttp(sendReqData);
        LoggerUtil.info(" ---- Test End ---- ");
    }

    /**
     * @param assertionResult
     * @return
     */
    private static ResponseAssertionResultDTO getResponseAssertionResult(AssertionResult assertionResult) {
        ResponseAssertionResultDTO responseAssertionResult = null;
        if (StringUtils.startsWith(assertionResult.getName(), "ErrorReportAssertion")) {
            responseAssertionResult = new ErrorReportAssertionResultDTO(assertionResult.getFailureMessage());
        } else {
            responseAssertionResult = new ResponseAssertionResultDTO();
        }
        responseAssertionResult.setName(assertionResult.getName());
        if (StringUtils.isNotEmpty(assertionResult.getName()) && assertionResult.getName().indexOf("split==") != -1) {
            if (assertionResult.getName().indexOf("JSR223") != -1) {
                String[] array = assertionResult.getName().split("split==", 3);
                if (array.length > 2 && "JSR223".equals(array[0])) {
                    responseAssertionResult.setName(array[1]);
                    if (array[2].indexOf("split&&") != -1) {
                        String[] content = array[2].split("split&&");
                        responseAssertionResult.setContent(content[0]);
                        if (content.length > 1) {
                            responseAssertionResult.setScript(content[1]);
                        }
                    } else {
                        responseAssertionResult.setContent(array[2]);
                    }
                }
            } else {
                String[] array = assertionResult.getName().split("split==");
                responseAssertionResult.setName(array[0]);
                StringBuffer content = new StringBuffer();
                for (int i = 1; i < array.length; i++) {
                    content.append(array[i]);
                }
                responseAssertionResult.setContent(content.toString());
            }
        }
        responseAssertionResult.setPass(!assertionResult.isFailure() && !assertionResult.isError());
        if (!responseAssertionResult.isPass()) {
            responseAssertionResult.setMessage(assertionResult.getFailureMessage());
        }
        return responseAssertionResult;
    }

    private static String getMethod(SampleResult result) {
        String body = result.getSamplerData();
        String start = "RPC Protocol: ";
        String end = "://";
        if (StringUtils.contains(body, start)) {
            String protocol = StringUtils.substringBetween(body, start, end);
            if (StringUtils.isNotEmpty(protocol)) {
                return protocol.toUpperCase();
            }
            return "DUBBO";
        } else if (StringUtils.contains(result.getResponseHeaders(), "url:jdbc")) {
            return "SQL";
        } else {
            String method = StringUtils.substringBefore(body, " ");
            for (HttpMethodConstants value : HttpMethodConstants.values()) {
                if (StringUtils.equals(method, value.name())) {
                    return method;
                }
            }
            return "Request";
        }
    }

    public void handlerResult(SampleResult sampleResult) {
        LoggerUtil.debug("开始处理结果资源【" + sampleResult.getSampleLabel() + "】");
        Class<? extends SampleResult> aClass = sampleResult.getClass();
        if (!aClass.getName().contains("http.sampler")) {
            LoggerUtil.info("非http请求【".concat(sampleResult.getSampleLabel()) + "】");
            this.labelPrefix.add(sampleResult.getSampleLabel());
            SampleResult[] subResults = sampleResult.getSubResults();
            if (subResults.length != 0) {
                for (SampleResult result : subResults) {
                    this.currentDeep += 1;
                    handlerResult(result);
                    this.currentDeep -= 1;
                }
            } else {
                LoggerUtil.info("非事务控制器：".concat(sampleResult.getSampleLabel()));
            }

        } else {
            LoggerUtil.info("LabelPrefix: ".concat(this.labelPrefix.toString()));
            TestCaseInfoDTO testCaseInfo = new TestCaseInfoDTO();
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
            // 超过20M的文件不入库
            long size = 1024 * 1024 * 20;
            if (StringUtils.equals(ContentType.APPLICATION_OCTET_STREAM.getMimeType(),
                    httpSampleResult.getContentType())
                    && StringUtils.isNotEmpty(httpSampleResult.getResponseDataAsString())
                    && httpSampleResult.getResponseDataAsString().length() > size) {
                testCaseInfo.setRequestBody("超过20M的RequestBody文件不入库!!!");
            } else {
                testCaseInfo.setRequestBody(httpSampleResult.getResponseDataAsString());
            }
            testCaseInfo.setErrorCount(httpSampleResult.getErrorCount());
            testCaseInfo.setSuccessful(httpSampleResult.isSuccessful());
            testCaseInfo.setCookies(httpSampleResult.getCookies());
            testCaseInfo.setRequestMethod(getMethod(httpSampleResult));
            testCaseInfo.setRequestSize(httpSampleResult.getSentBytes());
            testCaseInfo.setResponseCode(httpSampleResult.getResponseCode());
            testCaseInfo.setResponseHeader(httpSampleResult.getResponseHeaders());
            testCaseInfo.setResponseBody(httpSampleResult.getResponseDataAsString());
            testCaseInfo.setConnectTime(httpSampleResult.getConnectTime());
            testCaseInfo.setLatency(httpSampleResult.getLatency());
            testCaseInfo.setTotalAssertions(httpSampleResult.getAssertionResults().length);
            testCaseInfo.setGroupThreads(httpSampleResult.getGroupThreads());
            testCaseInfo.setAllThreads(httpSampleResult.getAllThreads());
            testCaseInfo.setDataEncoding(httpSampleResult.getDataEncodingNoDefault());
            testCaseInfo.setIgnore(httpSampleResult.isIgnore());

            for (AssertionResult assertionResult : sampleResult.getAssertionResults()) {
                StringBuilder sb = new StringBuilder();
                ResponseAssertionResultDTO responseAssertionResult = getResponseAssertionResult(assertionResult);
                if (responseAssertionResult.isPass()) {
                    testCaseInfo.addPassAssertions();
                } else {
                    sb.append(assertionResult.getName()).append(": ").append(assertionResult.getFailureMessage())
                            .append("\n");
                }
                // xpath 提取错误会添加断言错误
                if (StringUtils.isBlank(responseAssertionResult.getMessage()) ||
                        (StringUtils.isNotBlank(responseAssertionResult.getName())
                                && !responseAssertionResult.getName().endsWith("XPath2Extractor"))
                        || (StringUtils.isNotBlank(responseAssertionResult.getContent())
                                && !responseAssertionResult.getContent().endsWith("XPath2Extractor"))) {
                    testCaseInfo.getAssertions().add(responseAssertionResult);
                }

            }
             if (Boolean.TRUE.equals(testCaseInfo.getSuccessful())) {
                this.countSuccess += 1;
            }
            testCaseInfo.setFailMessage(testCaseInfo.toString());
            this.testCases.add(testCaseInfo);
            LoggerUtil.debug("处理结果资源【" + httpSampleResult.getSampleLabel() + "】结束");
        }
    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext backendListenerContext) {
        LoggerUtil.info("接收到JMETER执行数据【" + sampleResults.size() + " 】", testSummary.getBatchNo());
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

    private void sendHttp(SendReqDataDTO sendReqData) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setPrettyPrinting().create();
        String prettyJson = gson.toJson(sendReqData);
        LoggerUtil.info("SendReqData===========》\n".concat(prettyJson));
        HttpResponse<JsonNode> response = null;
        String message = "数据推送给Pika";
        try {
            response = Unirest.post(PikaBackendListenerClient.pikaServerUrl)
                    .header("Content-Type", "application/json")
                    .body(prettyJson)
                    .asJson();
            LoggerUtil.info(message.concat("成功：").concat(response.getBody().toString()));
        } catch (UnirestException e) {
            LoggerUtil.error(message.concat("异常：").concat(e.getMessage()));
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