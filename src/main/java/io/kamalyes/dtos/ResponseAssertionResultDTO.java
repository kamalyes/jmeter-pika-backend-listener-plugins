package io.kamalyes.dtos;

import lombok.Data;

@Data
public class ResponseAssertionResultDTO {

    private String name;

    private String content;

    private String script;

    private String message;

    private boolean pass;
}
