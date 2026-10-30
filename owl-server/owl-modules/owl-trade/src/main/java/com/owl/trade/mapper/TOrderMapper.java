package com.owl.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owl.trade.entity.TOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TOrderMapper extends BaseMapper<TOrder> {

    /**
     * 当天已用到的最大序号。
     *
     * 单号格式 OWL + yyyyMMdd + 6 位序号，前缀 11 位，序号从第 12 位开始。
     * 用途：进程内 KVStore 重启后序号会从 1 重来，先跟库里对齐再发号，
     * 否则会撞 uk_order_no。
     */
    @Select("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(order_no, 12) AS UNSIGNED)), 0)
            FROM t_order
            WHERE order_no LIKE CONCAT(#{prefix}, '%')
            """)
    Integer maxSeqOfPrefix(@Param("prefix") String prefix);
}
