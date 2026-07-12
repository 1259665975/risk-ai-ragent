package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 图片 OCR 服务测试，验证视觉模型响应能被提取为可入库文本。
 */
@ExtendWith(MockitoExtension.class)
class ImageTextExtractorServiceTest {

    @Mock
    private ChatModel chatModel;

    private ImageTextExtractorService imageTextExtractorService;

    @BeforeEach
    void setUp() {
        imageTextExtractorService = new ImageTextExtractorService(chatModel, new RagProperties());
    }

    @Test
    void rejectsModelEmptyMarkerResponse() {
        mockOcrResponse("无文字内容");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> imageTextExtractorService.extract(new byte[]{1, 2, 3}, "scan.png"));
        assertTrueMessageContains(ex, "未识别到可用文字");
    }

    @Test
    void acceptsMeaningfulOcrResponse() {
        mockOcrResponse("第一章 授信审批流程\n1. 初审\n2. 复审");
        String text = imageTextExtractorService.extract(new byte[]{1, 2, 3}, "scan.png");
        assertEquals("第一章 授信审批流程\n1. 初审\n2. 复审", text);
    }

    private void mockOcrResponse(String answer) {
        AssistantMessage assistantMessage = new AssistantMessage(answer);
        Generation generation = new Generation(assistantMessage);
        ChatResponse response = new ChatResponse(java.util.List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
    }

    private static void assertTrueMessageContains(BusinessException ex, String fragment) {
        if (ex.getMessage() == null || !ex.getMessage().contains(fragment)) {
            throw new AssertionError("expected message containing: " + fragment + ", got: " + ex.getMessage());
        }
    }
}
