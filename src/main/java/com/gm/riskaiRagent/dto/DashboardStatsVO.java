package com.gm.riskaiRagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘汇总视图对象，承载用户数、文档数、问答数等核心指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsVO {

    private long documents;
    private long users;
    private long ragentCount;
    private long categories;
}
