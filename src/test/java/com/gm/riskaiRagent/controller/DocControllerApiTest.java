package com.gm.riskaiRagent.controller;

import com.gm.riskaiRagent.common.GlobalExceptionHandler;
import com.gm.riskaiRagent.dto.IngestResponse;
import com.gm.riskaiRagent.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文档开放接口测试，验证上传校验、支持类型查询和清空接口行为。
 */
@ExtendWith(MockitoExtension.class)
class DocControllerApiTest {

    @Mock
    private DocumentService documentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DocController controller = new DocController(documentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void supportedTypesReturnsCatalog() throws Exception {
        mockMvc.perform(get("/doc/supported-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.all.length()").value(greaterThan(20)))
                .andExpect(jsonPath("$.data.image", hasItem("png")))
                .andExpect(jsonPath("$.data.accept", containsString(".pdf")));
    }

    @Test
    void ingestRejectsEmptyFile() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/doc/ingest").file(empty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("不能为空")));
    }

    @Test
    void ingestSuccess() throws Exception {
        when(documentService.ingest(any())).thenReturn(IngestResponse.builder()
                .fileName("policy.txt")
                .docId("abc123")
                .charCount(10)
                .tokenCount(3)
                .chunkCount(1)
                .fileType("txt")
                .parseMode("TIKA")
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "policy.txt", "text/plain",
                "风控规则".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/doc/ingest").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("policy.txt"))
                .andExpect(jsonPath("$.data.parseMode").value("TIKA"))
                .andExpect(jsonPath("$.data.fileType").value("txt"));

        verify(documentService).ingest(any());
    }

    @Test
    void clearReturnsRemovedCount() throws Exception {
        when(documentService.clearAll()).thenReturn(5L);

        mockMvc.perform(delete("/doc/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.removedChunks").value(5));
    }
}
