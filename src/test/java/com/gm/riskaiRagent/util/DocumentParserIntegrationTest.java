package com.gm.riskaiRagent.util;

import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.service.ImageTextExtractorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文档解析集成测试，覆盖 Tika 和图片 OCR 路由等解析场景。
 */
@ExtendWith(MockitoExtension.class)
class DocumentParserIntegrationTest {

    @Mock
    private ImageTextExtractorService imageTextExtractorService;

    private DocumentParser documentParser;

    @BeforeEach
    void setUp() {
        documentParser = new DocumentParser(imageTextExtractorService);
    }

    @Test
    void parsesPlainTextWithTika() {
        byte[] bytes = "第一条 授信审批须在三个工作日内完成。".getBytes(StandardCharsets.UTF_8);

        DocumentParser.ParseResult result = documentParser.parse(bytes, "rules.txt");

        assertEquals("TIKA", result.parseMode());
        assertEquals("txt", result.fileType());
        assertTrue(result.text().contains("授信审批"));
        verify(imageTextExtractorService, never()).extract(any(), any());
    }

    @Test
    void routesImageToOcr() {
        when(imageTextExtractorService.extract(any(), eq("scan.png")))
                .thenReturn("印章下方文字：风险缓释措施");

        DocumentParser.ParseResult result = documentParser.parse(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "scan.png");

        assertEquals("VISION_OCR", result.parseMode());
        assertEquals("png", result.fileType());
        verify(imageTextExtractorService).extract(any(), eq("scan.png"));
    }

    @Test
    void rejectsUnsupportedExtension() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> documentParser.parse("x".getBytes(), "virus.exe"));
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }
}
