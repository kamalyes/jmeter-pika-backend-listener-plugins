package io.kamalyes.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TestCaseInfoDTO {
    /**
     * 模块名称
     */
    private String moduleName;
    /**
     * 用例名称
     */
    private String caseName;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 请求地址
     */
    private String requestUrl;
    /**
     * 请求方式
     */
    private String requestMethod;
    /**
     * cookies
     */
    private String cookies;
    /**
     * 请求数据大小
     */
    private long requestSize;
    /**
     * 请求Headers
     */
    private String requestHeader;

    /**
     * 请求Body
     */
    private String requestQuery;

    /**
     * 请求Body
     */
    private String requestBody;

    /**
     * 请求头大小
     */
    private int headersSize = 0;

    /**
     * 请求体大小
     */
    private long bodySize = 0;


    /**
     * 响应Code
     */
    private String responseCode;
    /**
     * 响应时间 jmeter 指向elapsedTime 即经过的时间(= Sample time = Load time = Response time )
     * https://stackoverflow.com/questions/63449594/jmeter-sampler-result-understanding-load-time-connect-time-and-latency
     *  https://jmeter.apache.org/usermanual/glossary.html
     */
    private long responseTime;
    /**
     * 响应包体大小
     */
    private long responseSize;
    /**
     * 响应Headers
     */
    private String responseHeader;
    /**
     * 响应Body
     */
    private String responseBody;
    /**
     * 错误原因
     */
    private String failMessage;
    /**
     * 失败数量
     */
    private int errorCount = 0;
    /**
     * 是否通过、成功（符合预期断言）
     */
    private Boolean successful;
    /**
     * 连接时间 不常用，请求连接建立的时间，这个时间 < Latency time < Elapsed time

     */
    private long connectTime;
    /**
     * 延迟时间 不常用，表示请求发送到刚开始接收响应时，这个时间<Elapsed time

     */
    private long latency;
    /**
     * 线程组数
     */
    private volatile int groupThreads;
    /**
     * 所有线程数
     */
    private volatile int allThreads;
    /**
     * 数据编码类型
     */
    private String dataEncoding;
    /**
     * 是否忽略
     */
    private Boolean ignore;
    /**
     * 多少个断言点
     */
    private int totalAssertions = 0;
    /**
     * 断言成功数量
     */
    private int passAssertions = 0;
    /**
     * 结果断言
     */
    private final List<ResponseAssertionResultDTO> assertions = new ArrayList<>();
    
    public void addPassAssertions() {
        this.passAssertions++;
    }
}
