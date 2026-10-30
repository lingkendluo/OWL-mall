package com.owl.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_usage_log")
public class AiUsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 匿名对话为 null */
    private Long userId;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    /** 必填，用来定位"偶发很慢" */
    private Integer latencyMs;

    /** 1 成功 0 失败 */
    private Integer success;

    private LocalDateTime createdAt;
}
