package com.owl.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.owl.admin.service.StatsService;
import com.owl.admin.vo.StatsVO;
import com.owl.auth.entity.MUser;
import com.owl.auth.mapper.MUserMapper;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.common.util.PriceUtil;
import com.owl.trade.entity.TOrder;
import com.owl.trade.mapper.TOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int LOW_STOCK_THRESHOLD = 10;

    /** 只统计已支付未退款，退款单是 pay_status=2，天然被排除 */
    private static final String GMV_EXPR =
            "COALESCE(SUM(CASE WHEN pay_status = 1 THEN pay_amount_cent ELSE 0 END), 0) AS gmv";

    private final TOrderMapper orderMapper;
    private final MUserMapper userMapper;
    private final CProductMapper productMapper;

    @Override
    public StatsVO overview() {
        Map<String, Object> all = sumOrders(null);
        LocalDateTime todayStart = LocalDate.now(ZONE).atStartOfDay();
        Map<String, Object> today = sumOrders(todayStart);

        StatsVO vo = new StatsVO();
        vo.setGmv(PriceUtil.toYuan(num(all, "gmv")));
        vo.setOrderCount(num(all, "cnt"));
        vo.setTodayGmv(PriceUtil.toYuan(num(today, "gmv")));
        vo.setTodayOrderCount(num(today, "cnt"));
        // 游客账号不算进「用户数」，否则运营看到的数字全是清理前的僵尸号
        vo.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<MUser>()
                .eq(MUser::getIsGuest, 0)));
        vo.setProductCount(productMapper.selectCount(new LambdaQueryWrapper<CProduct>()));
        vo.setLowStockCount(Objects.requireNonNullElse(productMapper.countLowStock(LOW_STOCK_THRESHOLD), 0L));
        return vo;
    }

    private Map<String, Object> sumOrders(LocalDateTime since) {
        QueryWrapper<TOrder> wrapper = new QueryWrapper<TOrder>()
                .select("COUNT(*) AS cnt", GMV_EXPR);
        if (since != null) {
            wrapper.ge("created_at", since);
        }
        List<Map<String, Object>> rows = orderMapper.selectMaps(wrapper);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private long num(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
