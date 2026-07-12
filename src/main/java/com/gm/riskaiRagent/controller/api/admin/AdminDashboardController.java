package com.gm.riskaiRagent.controller.api.admin;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.DashboardChartsVO;
import com.gm.riskaiRagent.dto.DashboardStatsVO;
import com.gm.riskaiRagent.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端仪表盘接口，汇总用户、文档、分类和问答统计数据。
 */
@Tag(name = "AdminDashboard")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.stats());
    }

    @GetMapping("/charts")
    public Result<DashboardChartsVO> charts() {
        return Result.success(dashboardService.charts());
    }
}
