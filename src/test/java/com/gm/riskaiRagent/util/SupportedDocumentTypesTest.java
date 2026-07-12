package com.gm.riskaiRagent.util;

import com.gm.riskaiRagent.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件类型白名单测试，验证支持格式、图片识别和扩展名解析。
 */
class SupportedDocumentTypesTest {

    @Test
    void recognizesOfficeAndImageExtensions() {
        assertTrue(SupportedDocumentTypes.isSupported("policy.docx"));
        assertTrue(SupportedDocumentTypes.isSupported("data.XLSX"));
        assertTrue(SupportedDocumentTypes.isSupported("scan.PNG"));
        assertTrue(SupportedDocumentTypes.isImage("photo.jpeg"));
        assertFalse(SupportedDocumentTypes.isImage("notes.txt"));
    }

    @Test
    void rejectsUnknownExtension() {
        assertFalse(SupportedDocumentTypes.isSupported("archive.zip"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> SupportedDocumentTypes.validate("malware.exe"));
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void rejectsBlankFileName() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> SupportedDocumentTypes.validate(null));
        assertEquals("文件名不能为空", ex.getMessage());
    }
}
