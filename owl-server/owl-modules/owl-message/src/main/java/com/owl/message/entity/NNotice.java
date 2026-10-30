package com.owl.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城公告。和 n_message 的区别：站内信是「一人一封」，公告是「一份广播」，
 * 用户端只读已发布的，管理端才看得到草稿。
 */
@Data
@TableName("n_notice")
public class NNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String body;

    /** 1 公告 2 活动 3 维护 4 上新 */
    private Integer type;

    /** 0 普通 1 置顶 */
    private Integer pinned;

    /** 0 草稿 1 已发布 2 已下线 */
    private Integer status;

    private Long adminId;

    private LocalDateTime publishAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
