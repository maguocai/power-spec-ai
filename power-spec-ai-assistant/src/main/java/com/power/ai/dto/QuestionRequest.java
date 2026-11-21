package com.power.ai.dto;

import lombok.Data;

@Data
public class QuestionRequest {

    // 用户问题
    private String question;

    // 可选：指定规范类型，如"储能技术监督"、"电网安全规程"
    private String specType;
}
