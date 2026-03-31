package com.fang.langchaindemo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 17:50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    /**
     *  姓名
     */
    private String name;

    /**
     *  年龄
     */
    private int age;

    /**
     *  出生日期
     */
    private String birthDay;

    /**
     *  是否在世
     */
    private boolean isAlive;

    /**
     *  死亡日期(如果已故)
     */
    private String deathDate;

    /**
     *  学历
     */
    private String degree;
}
