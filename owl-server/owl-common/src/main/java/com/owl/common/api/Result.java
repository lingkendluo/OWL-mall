package com.owl.common.api;

import com.owl.common.api.ErrorCode;
import lombok.Data;
import org.slf4j.MDC;

/**
 * 统一响应体。code=0 即成功；业务失败 HTTP 仍是 200（401/403 除外）。
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> Result<T> ok() {
        return of(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return of(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return of(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return of(code, message, null);
    }

    private static <T> Result<T> of(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        // traceId 由 TraceFilter 写入 MDC，响应体里带上便于前后端对账
        result.traceId = MDC.get("traceId");
        return result;
    }
}
