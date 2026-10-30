package com.owl.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m_user")
public class MUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String email;

    private String passwordHash;

    /** 对应前端 AuthUser.name */
    private String nickname;

    private String avatar;

    /** black/normal/guest */
    private String level;

    private String memberNo;

    private Integer points;

    private Long balanceCent;

    private Integer isGuest;

    /** 1 正常 0 禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
