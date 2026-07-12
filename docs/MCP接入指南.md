# MCP 接入指南

本项目已集成 **Spring AI MCP Server**（SSE / WebMVC），将风控 RAG 能力以 **MCP 工具** 形式暴露，供 Cursor、Claude Desktop、其他 MCP 客户端调用。

## 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/sse` | GET | MCP SSE 连接（客户端订阅） |
| `/mcp/message` | POST | MCP 消息收发 |

服务默认地址：`http://localhost:8080`（启动后端后可用）。

## 暴露的工具

| 工具名 | 说明 |
|--------|------|
| `searchRiskKnowledge` | 向量检索知识库片段（不调用大模型） |
| `askRiskQuestion` | 完整 RAG 风控问答（检索 + 千问 + 防幻觉 Prompt） |
| `getKnowledgeBaseStats` | 知识库统计（文档数、问答次数等） |
| `listKnowledgeCategories` | 列出知识分类 |

## Cursor 配置示例

在项目或用户目录创建/编辑 MCP 配置（Cursor Settings → MCP），或使用 `~/.cursor/mcp.json`：

```json
{
  "mcpServers": {
    "risk-ai-ragent": {
	  "type": "sse",
      "url": "http://localhost:8080"
    }
  }
}
```

> 部分 MCP 客户端使用 `type: "sse"` 或 Streamable HTTP，以客户端文档为准。Spring AI 1.0 默认提供 **SSE** 传输。

~~~
现象根因（Cursor SSE 客户端底层行为）
Cursor 建立 MCP 连接完整流程：
GET /sse → 创建 SSE 长连接，接收sessionId ✅
Cursor额外自动预探 / 重试发送 POST /sse（这是 Cursor 自身错误逻辑）
✅ 存在@PostMapping("/sse") → 返回405 Method Not Allowed，客户端识别：路径存在，只是方法不对，继续正常走后续消息流程
❌ 删除 Controller 后 → POST /sse 返回404 Not Found
Cursor 判定服务地址不可用，直接主动断开刚建立好的 SSE 通道，导致连接失败、变黄灯。
重点区分：
405 = 资源存在，请求方式错误 → Cursor不会判定服务失效
404 = 路径不存在 → Cursor 直接判定服务异常，掐断连接

> 部分 MCP 客户端使用 `type: "sse"` 或 Streamable HTTP，以客户端文档为准。Spring AI 1.0 默认提供 **SSE** 传输。
~~~

![image-20260712164923521](C:\Users\q1259\AppData\Roaming\Typora\typora-user-images\image-20260712164923521.png)

## Claude Desktop 配置示例（可选）

`%APPDATA%\Claude\claude_desktop_config.json`（Windows）：

```json
{
  "mcpServers": {
    "risk-ai-ragent": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/sse"]
    }
  }
}
```

需本机已安装 Node.js；`mcp-remote` 将远程 SSE 转为 stdio 供 Claude Desktop 使用。

## 配置项（application.yml）

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        name: risk-ai-ragent-mcp
        sse-endpoint: /sse
        sse-message-endpoint: /mcp/message
```

关闭 MCP：`spring.ai.mcp.server.enabled=false`

## 与多跳检索的关系

| 工具 | 检索方式 |
|------|----------|
| `searchRiskKnowledge` | 始终**单跳** |
| `askRiskQuestion` | 走 `RagRagentService`，受 `risk-ai.multi-hop.enabled` 控制 |

详见 [多跳检索指南.md](多跳检索指南.md)。

## 与 A2A 协议的关系

| 能力 | 说明 |
|------|------|
| **MCP** | 单 Agent 对外暴露工具（检索、问答等），供 Cursor 等客户端调用 |
| **A2A** | 多 Agent 注册、发现、消息路由与任务追踪，供平台内 Agent 协同 |

可将 MCP SSE 地址（`http://localhost:8080/sse`）注册为 A2A Agent 的 endpoint。详见 [A2A协议指南.md](A2A协议指南.md)。

## 注意事项

1. MCP 端点**不走** `/api/**` 鉴权拦截，本地开发可直接连接；生产环境建议加网关鉴权或 IP 白名单。
2. 调用 `askRiskQuestion` 会消耗大模型 Token，与 Web 端 `/risk/ragent` 共用同一套 RAG 逻辑（含多跳配置）。
3. `searchRiskKnowledge` 始终为**单跳**检索，不受 `multi-hop.enabled` 影响。
4. 需先启动 MySQL / Redis / Milvus，并完成文档入库，检索与问答才有数据。
