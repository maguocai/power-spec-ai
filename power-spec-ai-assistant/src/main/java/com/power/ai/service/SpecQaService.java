package com.power.ai.service;

import com.power.ai.dto.AnswerResponse;

public interface SpecQaService {
    AnswerResponse ask(String question);
}
