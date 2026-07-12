package com.gm.riskaiRagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gm.riskaiRagent.dto.DashboardChartsVO;
import com.gm.riskaiRagent.dto.DashboardStatsVO;
import com.gm.riskaiRagent.entity.RagentLog;
import com.gm.riskaiRagent.entity.SysCategory;
import com.gm.riskaiRagent.entity.SysDocument;
import com.gm.riskaiRagent.mapper.RagentLogMapper;
import com.gm.riskaiRagent.mapper.SysCategoryMapper;
import com.gm.riskaiRagent.mapper.SysDocumentMapper;
import com.gm.riskaiRagent.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘服务，聚合用户、文档、分类和问答日志的统计数据。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final SysDocumentMapper sysDocumentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysCategoryMapper sysCategoryMapper;
    private final RagentLogMapper ragentLogMapper;

    public DashboardStatsVO stats() {
        long documents = countOrZero(sysDocumentMapper.selectCount(null));
        long users = countOrZero(sysUserMapper.selectCount(null));
        long categories = countOrZero(sysCategoryMapper.selectCount(null));
        long ragentCount = countOrZero(ragentLogMapper.selectCount(null));
        return DashboardStatsVO.builder()
                .documents(documents)
                .users(users)
                .ragentCount(ragentCount)
                .categories(categories)
                .build();
    }

    public DashboardChartsVO charts() {
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            Long count = ragentLogMapper.selectCount(new LambdaQueryWrapper<RagentLog>()
                    .ge(RagentLog::getCreatedAt, start)
                    .lt(RagentLog::getCreatedAt, end));
            dates.add(day.format(DAY_FMT));
            counts.add(countOrZero(count));
        }

        Map<String, List<?>> trend = new HashMap<>();
        trend.put("dates", dates);
        trend.put("counts", counts);

        List<Map<String, Object>> categoryDistribution = new ArrayList<>();
        for (SysCategory category : sysCategoryMapper.selectList(null)) {
            Long count = sysDocumentMapper.selectCount(new LambdaQueryWrapper<SysDocument>()
                    .eq(SysDocument::getCategoryId, category.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", category.getName());
            item.put("value", countOrZero(count));
            categoryDistribution.add(item);
        }

        return DashboardChartsVO.builder()
                .trend(trend)
                .categoryDistribution(categoryDistribution)
                .build();
    }

    private long countOrZero(Long count) {
        return count == null ? 0L : count;
    }
}
