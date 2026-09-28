package com.docqa.framework.parser;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析：按章节切分，保留标题层级与页码
 */
public class DocumentParser {

    public record ParsedDocument(List<Section> sections, String fullText) {
        public int sectionCount() { return sections == null ? 0 : sections.size(); }
    }

    public record Section(String heading, int level, String text, Integer pageNo) { }

    public interface Parser {
        boolean supports(String sourceType);
        ParsedDocument parse(InputStream in) throws Exception;
    }
}
