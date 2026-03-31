package com.fang.langchaindemo.controller;

import com.fang.langchaindemo.domain.enums.CustomerServiceCategory;
import com.fang.langchaindemo.service.CustomerServiceCategoryClassifier;
import dev.langchain4j.classification.EmbeddingModelTextClassifier;
import dev.langchain4j.classification.TextClassifier;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fang.langchaindemo.domain.enums.CustomerServiceCategory.*;
import static java.util.Arrays.asList;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 17:56
 */
@Slf4j
@RestController
public class TextClassifyController {

    @Resource
    @Qualifier("ollamaChatModel")
    private OllamaChatModel ollamaChatModel;

    @Resource
    @Qualifier("ollamaEmbeddingModel")
    private OllamaEmbeddingModel ollamaEmbeddingModel;

    @GetMapping(value = "/classify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> classify(@RequestParam String query) {
        try {
            CustomerServiceCategoryClassifier customerServiceClassifier = AiServices.create(CustomerServiceCategoryClassifier.class, ollamaChatModel);
            CustomerServiceCategory classify = customerServiceClassifier.classify(query);
            return ResponseEntity.ok(classify.getDesc());
        } catch (Exception e) {
            log.error("classify", e);
            return ResponseEntity.ok("{\"error\":\"classify error: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/classify/embed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> classifyEmbed(@RequestParam String query) {
        try {
            TextClassifier<CustomerServiceCategory> classifier = new EmbeddingModelTextClassifier<>(ollamaEmbeddingModel, getExamples());
            List<CustomerServiceCategory> categories = classifier.classify(query);
            String result = "";
            if (!CollectionUtils.isEmpty(categories)) {
                CustomerServiceCategory classify = categories.get(0);
                result += classify.getDesc() + ",";
            }
            return ResponseEntity.ok(StringUtils.trimTrailingCharacter(result, ','));
        } catch (Exception e) {
            log.error("classify", e);
            return ResponseEntity.ok("{\"error\":\"classify error: " + e.getMessage() + "\"}");
        }
    }

    Map<CustomerServiceCategory, List<String>> getExamples() {
        Map<CustomerServiceCategory, List<String>> examples = new HashMap<>();

        examples.put(PRODUCT, asList(
                "怎么没有产品说明书？",
                "产品的保修期过了怎么办？",
                "我新买的东西用了一周就坏了？",
                "产品无法使用？"
        ));

        examples.put(ORDER, asList(
                "我的订单现在到哪里了？",
                "能给我一个快递单号吗？",
                "我怎么知道我的订单已经发货了？",
                "我可以更改配送方式吗？",
                "你们提供次日达服务吗？",
                "可以选择到店自提吗？",
                "我的订单什么时候能到？",
                "为什么我的配送延迟了？",
                "我可以指定配送日期吗？",
                "已经过了预计送达日期，我的订单在哪里？",
                "如果出现延迟，我会收到通知吗？",
                "天气原因会导致配送延迟多久？",
                "我收到了订单，但少了一件商品。",
                "包裹送达时是空的。",
                "我收到的商品错了，该怎么办？",
                "我所有的商品会同时送达吗？",
                "为什么我只收到部分订单商品？",
                "剩下的商品能更快送达吗？"
        ));

        examples.put(PAYMENT, asList(
                "能用支付宝吗?",
                "微信支付可以吗?",
                "支持信用卡吗?",
                "付款时我遇到一个错误",
                "可以通过银行转账付款吗？",
                "为什么我的付款被拒绝了",
                "可以给我发送上一笔订单的发票吗？",
                "发票会自动发送到我的电子邮箱吗？",
                "如何申请退款？"
        ));

        examples.put(MEMBER, asList(
                "我的会员等级变低了?",
                "我的积分过期了?",
                "我的优惠券不能用了?",
                "能给我发个支付95折优惠券吗？",
                "满减优惠券当次消费就能用吗？",
                "能送100点优惠积分吗？"
        ));

        examples.put(ACCOUNT, asList(
                "如何注销账号?",
                "我的密码过期了?",
                "为什么无法登录",
                "登录时手机收不到验证码",
                "如果更换账号绑定的手机号"
        ));

        examples.put(OTHERS, asList(
                "厂商的联系电话是多少?",
                "如何加盟?",
                "你们公司还招人吗？"
        ));

        return examples;
    }
}
