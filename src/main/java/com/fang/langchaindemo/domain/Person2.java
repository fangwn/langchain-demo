package com.fang.langchaindemo.domain;

import dev.langchain4j.model.output.structured.Description;

import java.time.LocalDate;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 16:36
 */
public class Person2 {

    @Description("first name of a person") // you can add an optional description to help an LLM have a better understanding
    String firstName;
    String lastName;
    LocalDate birthDate;
    Address address;
}
