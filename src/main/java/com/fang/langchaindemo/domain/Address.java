package com.fang.langchaindemo.domain;

import dev.langchain4j.model.output.structured.Description;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 16:36
 */
@Description("an address")
public class Address {
    String street;
    Integer streetNumber;
    String city;
}
