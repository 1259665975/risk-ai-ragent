package com.gm.riskaiRagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gm.riskaiRagent.dto.*;
import com.gm.riskaiRagent.entity.A2aAgent;
import com.gm.riskaiRagent.entity.A2aTask;
import com.gm.riskaiRagent.mapper.A2aAgentMapper;
import com.gm.riskaiRagent.mapper.A2aTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class A2AProtocolService {

    private final A2aAgentMapper agentMapper;
    private final A2aTaskMapper taskMapper;

    public Map<String, Object> getOverview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "A2A Protocol");
        data.put("title", "A2A 协议中心");
        data.put("description", "用于统一展示 Agent-to-Agent 协议概念、消息结构、交互流程与接入建议，方便后续扩展多 Agent 协同能力。");
        data.put("principles", List.of("Agent 能力描述与发现", "任务路由与协商", "消息格式统一", "异步回调与状态追踪"));
        data.put("suggestions", List.of("先定义 Agent 元数据与能力清单。", "再约定请求、响应与错误结构。", "最后补充鉴权、审计和限流。"));
        data.put("status", "draft");
        data.put("registeredAgents", agentMapper.selectCount(new LambdaQueryWrapper<>()).intValue());
        return data;
    }

    public PageResult<A2AAgentDTO> pageAgents(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<A2aAgent> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like(A2aAgent::getName, keyword).or().like(A2aAgent::getDescription, keyword));
        }
        wrapper.orderByDesc(A2aAgent::getCreatedAt);
        IPage<A2aAgent> page = agentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<A2AAgentDTO> records = new ArrayList<>();
        for (A2aAgent entity : page.getRecords()) {
            records.add(toAgentDTO(entity));
        }
        return PageResult.of(page.getTotal(), pageNum, pageSize, records);
    }

    public PageResult<A2ATaskDTO> pageTasks(long pageNum, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<A2aTask> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like(A2aTask::getTaskId, keyword).or().like(A2aTask::getMessage, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(A2aTask::getStatus, status);
        }
        wrapper.orderByDesc(A2aTask::getCreatedAt);
        IPage<A2aTask> page = taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<A2ATaskDTO> records = new ArrayList<>();
        for (A2aTask entity : page.getRecords()) {
            records.add(toTaskDTO(entity));
        }
        return PageResult.of(page.getTotal(), pageNum, pageSize, records);
    }

    public List<A2AAgentDTO> listAgents() {
        return pageAgents(1, 1000, null).getRecords();
    }

    @Transactional(rollbackFor = Exception.class)
    public A2AAgentDTO register(A2ARegisterRequest request) {
        return saveAgent(new A2aAgent(), request.getName(), request.getDescription(), request.getCapabilities(), request.getEndpoint(), "ACTIVE");
    }

    @Transactional(rollbackFor = Exception.class)
    public A2AAgentDTO update(String agentId, A2AUpdateRequest request) {
        A2aAgent entity = findAgent(agentId);
        return saveAgent(entity, request.getName(), request.getDescription(), request.getCapabilities(), request.getEndpoint(), request.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String agentId) {
        A2aAgent entity = findAgent(agentId);
        agentMapper.deleteById(entity.getId());
    }

    public A2AConnectivityDTO testConnectivity(String agentId) {
        A2aAgent agent = findAgent(agentId);
        A2AConnectivityDTO dto = new A2AConnectivityDTO();
        dto.setAgentId(agent.getAgentId());
        dto.setEndpoint(agent.getEndpoint());
        dto.setCheckedAt(LocalDateTime.now());
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(agent.getEndpoint()))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .header("Accept", "text/event-stream")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            dto.setConnected(response.statusCode() >= 200 && response.statusCode() < 400);
            dto.setMessage("SSE 连接检测完成，HTTP 状态码: " + response.statusCode());
        } catch (Exception ex) {
            dto.setConnected(false);
            dto.setMessage("连通性检测失败: " + ex.getMessage());
        }
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> send(A2ASendRequest request) {
        A2aAgent from = findAgent(request.getFromAgentId());
        A2aAgent to = findAgent(request.getToAgentId());
        String taskId = UUID.randomUUID().toString().replace("-", "");
        A2aTask task = new A2aTask();
        task.setTaskId(taskId);
        task.setFromAgentId(from.getAgentId());
        task.setToAgentId(to.getAgentId());
        task.setMessage(request.getMessage());
        task.setStatus("PENDING");
        task.setDetailMessage("task accepted and queued");
        taskMapper.insert(task);
        asyncExecute(taskId, from.getName(), to.getName());
        return Map.of(
                "taskId", taskId,
                "fromAgentId", from.getAgentId(),
                "toAgentId", to.getAgentId(),
                "message", request.getMessage(),
                "status", "PENDING"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(String taskId) {
        A2aTask task = findTask(taskId);
        if ("COMPLETED".equals(task.getStatus())) {
            throw new IllegalArgumentException("已完成任务无法取消");
        }
        task.setStatus("CANCELLED");
        task.setDetailMessage("task cancelled by user");
        taskMapper.updateById(task);
    }

    public A2AStatusDTO getStatus(String taskId) {
        return toStatusDTO(findTask(taskId));
    }

    private void asyncExecute(String taskId, String fromName, String toName) {
        new Thread(() -> {
            try {
                Thread.sleep(1200L);
                A2aTask task = findTask(taskId);
                if ("CANCELLED".equals(task.getStatus())) {
                    return;
                }
                task.setStatus("RUNNING");
                task.setDetailMessage("task is running");
                taskMapper.updateById(task);

                Thread.sleep(1200L);
                task = findTask(taskId);
                if ("CANCELLED".equals(task.getStatus())) {
                    return;
                }
                task.setStatus("COMPLETED");
                task.setDetailMessage("message delivered from " + fromName + " to " + toName);
                taskMapper.updateById(task);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private A2aAgent findAgent(String agentId) {
        A2aAgent agent = agentMapper.selectOne(new LambdaQueryWrapper<A2aAgent>().eq(A2aAgent::getAgentId, agentId));
        if (agent == null) throw new IllegalArgumentException("agent 不存在或未注册");
        return agent;
    }

    private A2aTask findTask(String taskId) {
        A2aTask task = taskMapper.selectOne(new LambdaQueryWrapper<A2aTask>().eq(A2aTask::getTaskId, taskId));
        if (task == null) throw new IllegalArgumentException("task 不存在");
        return task;
    }

    private A2AAgentDTO saveAgent(A2aAgent entity, String name, String description, List<String> capabilities, String endpoint, String status) {
        if (entity.getAgentId() == null) {
            entity.setAgentId(UUID.randomUUID().toString().replace("-", ""));
        }
        entity.setName(name);
        entity.setDescription(description);
        entity.setCapabilities(String.join(",", capabilities));
        entity.setEndpoint(endpoint);
        entity.setStatus(status);
        if (entity.getId() == null) {
            agentMapper.insert(entity);
        } else {
            agentMapper.updateById(entity);
        }
        return toAgentDTO(entity);
    }

    private A2AAgentDTO toAgentDTO(A2aAgent entity) {
        A2AAgentDTO dto = new A2AAgentDTO();
        dto.setAgentId(entity.getAgentId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCapabilities(parseCsv(entity.getCapabilities()));
        dto.setEndpoint(entity.getEndpoint());
        dto.setStatus(entity.getStatus());
        dto.setRegisteredAt(entity.getCreatedAt());
        return dto;
    }

    private A2ATaskDTO toTaskDTO(A2aTask entity) {
        A2ATaskDTO dto = new A2ATaskDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setFromAgentId(entity.getFromAgentId());
        dto.setToAgentId(entity.getToAgentId());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus());
        dto.setDetailMessage(entity.getDetailMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private A2AStatusDTO toStatusDTO(A2aTask task) {
        A2AStatusDTO dto = new A2AStatusDTO();
        dto.setTaskId(task.getTaskId());
        dto.setStatus(task.getStatus());
        dto.setMessage(task.getDetailMessage());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
