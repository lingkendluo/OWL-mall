USE owl_mall;

-- 新人券模板：满 99 减 10
INSERT INTO mk_coupon (code, name, type, threshold_cent, discount_cent, start_at, end_at, total, received, status)
VALUES ('NEW10', '新人券 满 99 减 10', 1, 9900, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 100000, 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), threshold_cent = VALUES(threshold_cent);

-- 满 199 减 30（演示"我的券"有多张）
INSERT INTO mk_coupon (code, name, type, threshold_cent, discount_cent, start_at, end_at, total, received, status)
VALUES ('FULL30', '满 199 减 30', 1, 19900, 3000, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 50000, 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 公告样例：n_notice 没有业务唯一键，用 NOT EXISTS 兜住重复执行
INSERT INTO n_notice (title, body, type, pinned, status, publish_at)
SELECT '开张公告：全场百货一站买齐',
       'OWL 商城正式营业，零食、生鲜、日用、美妆、数码、家居共 15 个大类，下单后 48 小时内发货。',
       1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM n_notice WHERE title = '开张公告：全场百货一站买齐');

INSERT INTO n_notice (title, body, type, pinned, status, publish_at)
SELECT '活动：新人首单立减 10 元',
       '新注册用户可在「我的券」领取满 99 减 10 的新人券，有效期 365 天，与满 199 减 30 不可叠加。',
       2, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM n_notice WHERE title = '活动：新人首单立减 10 元');

INSERT INTO n_notice (title, body, type, pinned, status, publish_at)
SELECT '配送时效说明',
       '华东、华南地区当日 15:00 前付款的订单次日达，其余地区 2-3 天；生鲜类目走冷链，只发周边 300 公里。',
       1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM n_notice WHERE title = '配送时效说明');

