package com.owl.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.admin.entity.SAdmin;
import com.owl.admin.entity.SAdminLog;
import com.owl.admin.mapper.SAdminLogMapper;
import com.owl.admin.mapper.SAdminMapper;
import com.owl.admin.service.AdminLogService;
import com.owl.admin.vo.AdminLogVO;
import com.owl.common.api.PageResult;
import com.owl.common.context.AdminContext;
import com.owl.common.util.TimeTextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SAdminLogMapper adminLogMapper;
    private final SAdminMapper adminMapper;

    @Override
    public void record(String action, String target, String detail, String ip) {
        try {
            SAdminLog log = new SAdminLog();
            log.setAdminId(AdminContext.requireAdminId());
            log.setAction(action);
            log.setTarget(target);
            log.setDetail(detail);
            log.setIp(ip);
            log.setCreatedAt(LocalDateTime.now());
            adminLogMapper.insert(log);
        } catch (Exception e) {
            // 审计日志写失败不能把业务操作带崩
            log.warn("后台操作日志写入失败 action={} target={}", action, target, e);
        }
    }

    @Override
    public PageResult<AdminLogVO> page(String keyword, String action, int page, int size) {
        LambdaQueryWrapper<SAdminLog> wrapper = new LambdaQueryWrapper<SAdminLog>()
                .eq(action != null && !action.isBlank(), SAdminLog::getAction, action)
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(SAdminLog::getTarget, keyword)
                        .or()
                        .like(SAdminLog::getDetail, keyword))
                .orderByDesc(SAdminLog::getId);
        Page<SAdminLog> result = adminLogMapper.selectPage(Page.of(page, size), wrapper);
        if (result.getRecords().isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), page, size);
        }

        // 管理员表很小，一次全查出来做 id -> 账号 的映射，比逐条查省事
        Map<Long, String> names = adminMapper.selectList(new LambdaQueryWrapper<SAdmin>())
                .stream()
                .collect(Collectors.toMap(SAdmin::getId, SAdmin::getUsername, (a, b) -> a, java.util.HashMap::new));

        List<AdminLogVO> list = result.getRecords().stream().map(row -> {
            AdminLogVO vo = new AdminLogVO();
            vo.setId(String.valueOf(row.getId()));
            vo.setAdminName(names.getOrDefault(row.getAdminId(), "已删除账号"));
            vo.setAction(row.getAction());
            vo.setTarget(row.getTarget());
            vo.setDetail(row.getDetail());
            vo.setIp(row.getIp());
            vo.setCreatedAt(millis(row.getCreatedAt()));
            vo.setTime(TimeTextUtil.format(row.getCreatedAt()));
            return vo;
        }).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    private Long millis(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant().toEpochMilli();
    }
}
