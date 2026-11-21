package com.power.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerResponse {

    // 回答内容
    private String answer;

    // 回答依据的规范方向
    private String specCategory;

    // 耗时（毫秒）
    private Long costTime;

}

