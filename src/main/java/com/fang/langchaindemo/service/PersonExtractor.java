package com.fang.langchaindemo.service;

import com.fang.langchaindemo.domain.Person;
import dev.langchain4j.service.SystemMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 17:51
 */
public interface PersonExtractor {

    /**
     * 从生平介绍中提取人员主要信息
     *
     * @param biography 人员的生平介绍文本
     * @return 包含提取信息的Person对象
     */
    @SystemMessage("""
                你的任务是从生平介绍中，提取出该人的主要信息：
                name[姓名],age[年龄], birthDay[出生日期], isAlive[是否健在], deathDate[死亡日期(如果已逝世)], degree[最高学历]
                """)
    Person extractPerson(String biography);
}
