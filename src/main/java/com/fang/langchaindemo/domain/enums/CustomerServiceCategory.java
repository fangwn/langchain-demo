package com.fang.langchaindemo.domain.enums;

import lombok.Getter;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 17:54
 */
public enum CustomerServiceCategory {

    PRODUCT("产品相关"),
    ORDER("订单相关"),
    ACCOUNT("账户相关"),
    MEMBER("会员相关"),
    PAYMENT("支付相关"),
    OTHERS("其它问题");

    @Getter
    private final String desc;

    CustomerServiceCategory(String desc) {
        this.desc = desc;
    }
}
