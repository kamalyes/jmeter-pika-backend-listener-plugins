package io.kamalyes.dtos;

import lombok.Data;

@Data
public class ErrorReportAssertionResultDTO  extends  ResponseAssertionResultDTO{
    private String errorReportMessage;

    public ErrorReportAssertionResultDTO(String errorReportMessage){
        this.errorReportMessage = errorReportMessage;
        this.setMessage(errorReportMessage);
    }
}
