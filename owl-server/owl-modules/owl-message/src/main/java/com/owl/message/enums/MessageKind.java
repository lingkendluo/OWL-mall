package com.owl.message.enums;

import lombok.Getter;

/**
 * 站内信类型。取值直接对齐前端 Message.kind。
 */
@Getter
public enum MessageKind {

    ORDER("order"),
    PROMO("promo"),
    SYSTEM("system");

    private final String code;

    MessageKind(String code) {
        this.code = code;
    }

    public static MessageKind of(String value) {
        for (MessageKind kind : values()) {
            if (kind.code.equalsIgnoreCase(value)) {
                return kind;
            }
        }
        return SYSTEM;
    }
}
