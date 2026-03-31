package com.fang.langchaindemo.service;

import com.fang.langchaindemo.domain.enums.IssueCategory;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 23:58
 */
public interface HotelReviewIssueAnalyzer {

    @UserMessage("请分析以下评论:|||{{it}}|||")
    List<IssueCategory> analyzeReview(String review);
}
