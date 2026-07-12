package com.gm.riskaiRagent.controller.api.user;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.*;
import com.gm.riskaiRagent.service.A2AProtocolService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "UserA2A")
@RestController
@RequestMapping("/api/user/a2a")
@RequiredArgsConstructor
public class UserA2AController {

    private final A2AProtocolService a2aProtocolService;

    @GetMapping public Result<Map<String, Object>> detail() { return Result.success(a2aProtocolService.getOverview()); }
    @GetMapping("/agents") public Result<List<A2AAgentDTO>> listAgents() { return Result.success(a2aProtocolService.listAgents()); }
    @GetMapping("/agents/page") public Result<PageResult<A2AAgentDTO>> pageAgents(@RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String keyword) { return Result.success(a2aProtocolService.pageAgents(pageNum, pageSize, keyword)); }
    @PostMapping("/agents") public Result<A2AAgentDTO> register(@Valid @RequestBody A2ARegisterRequest request) { return Result.success(a2aProtocolService.register(request)); }
    @PutMapping("/agents/{agentId}") public Result<A2AAgentDTO> update(@PathVariable String agentId, @Valid @RequestBody A2AUpdateRequest request) { return Result.success(a2aProtocolService.update(agentId, request)); }
    @DeleteMapping("/agents/{agentId}") public Result<Void> delete(@PathVariable String agentId) { a2aProtocolService.delete(agentId); return Result.success(); }
    @GetMapping("/agents/{agentId}/connectivity") public Result<A2AConnectivityDTO> connectivity(@PathVariable String agentId) { return Result.success(a2aProtocolService.testConnectivity(agentId)); }
    @PostMapping("/messages") public Result<Map<String, Object>> send(@Valid @RequestBody A2ASendRequest request) { return Result.success(a2aProtocolService.send(request)); }
    @GetMapping("/tasks") public Result<PageResult<A2ATaskDTO>> pageTasks(@RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String keyword, @RequestParam(required = false) String status) { return Result.success(a2aProtocolService.pageTasks(pageNum, pageSize, keyword, status)); }
    @GetMapping("/tasks/{taskId}") public Result<A2AStatusDTO> status(@PathVariable String taskId) { return Result.success(a2aProtocolService.getStatus(taskId)); }
    @DeleteMapping("/tasks/{taskId}") public Result<Void> cancel(@PathVariable String taskId) { a2aProtocolService.cancelTask(taskId); return Result.success(); }
}
