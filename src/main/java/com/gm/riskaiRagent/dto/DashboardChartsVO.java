package com.gm.riskaiRagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘图表视图对象，承载前端折线图、柱状图等统计数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartsVO {

    private Map<String, List<?>> trend;
    private List<Map<String, Object>> categoryDistribution;
}
