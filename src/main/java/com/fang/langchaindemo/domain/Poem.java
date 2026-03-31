package com.fang.langchaindemo.domain;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 09:37
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Poem {

    @Description("标题")
    private String title;

    @Description("作者")
    private String author;

    @Description("内容")
    private String content;
}
