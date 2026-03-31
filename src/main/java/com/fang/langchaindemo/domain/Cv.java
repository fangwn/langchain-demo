package com.fang.langchaindemo.domain;

import dev.langchain4j.model.output.structured.Description;

/**
 *
 * @author fangwennan
 * @date 2026/3/25 10:16
 */
public class Cv {

    @Description("候选人的专业技能，逗号串接")
    private String skills;

    @Description("候选人的专业经历")
    private String professionalExperience;

    @Description("候选人的教育背景")
    private String studies;

    @Override
    public String toString() {
        return "CV:\n" +
                "专业技能 = \"" + skills + "\"\n" +
                "专业经历 = \"" + professionalExperience + "\"\n" +
                "教育背景 = \"" + studies + "\"\n";
    }
}
