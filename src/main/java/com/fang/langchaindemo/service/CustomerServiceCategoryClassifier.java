package com.fang.langchaindemo.service;

import com.fang.langchaindemo.domain.enums.CustomerServiceCategory;
import dev.langchain4j.service.UserMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 17:55
 */
public interface CustomerServiceCategoryClassifier {

    @UserMessage("将客人遇到的问题【{{text}}】归类")
    CustomerServiceCategory classify(String text);
}
