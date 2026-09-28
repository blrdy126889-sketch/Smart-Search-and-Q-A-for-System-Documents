package com.docqa.framework.parser;

import com.docqa.framework.parser.DocumentParser.ParsedDocument;
import com.docqa.framework.parser.DocumentParser.Parser;
import com.docqa.framework.parser.DocumentParser.Section;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word/DOCX 解析器：Tika 提取纯文本后按标题模式分节
 */
@Component
public class DocxParser implements Parser {

    private static final Pattern HEADING = Pattern.compile("^(第[一二三四五六七八九十百]+[章节条][、\\s].{0,60})$|^([一二三四五六七八九十]+[、.].{0,60})$");

    @Override
    public boolean supports(String sourceType) {
        return "DOC".equalsIgnoreCase(sourceType) || "DOCX".equalsIgnoreCase(sourceType);
    }

    @Override
    public ParsedDocument parse(InputStream in) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        parser.parse(in, handler, metadata);

        String text = handler.toString();
        String[] lines = text.split("\\R");
        List<Section> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String heading = "";
        int level = 1;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            Matcher m = HEADING.matcher(line);
            if (m.matches() && line.length() <= 60) {
                if (!current.isEmpty()) {
                    sections.add(new Section(heading, level, current.toString().trim(), null));
                    current = new StringBuilder();
                }
                heading = m.group(1) != null ? m.group(1).trim() : m.group(2).trim();
                level = m.group(1) != null ? 2 : 3;
            } else {
                if (!current.isEmpty()) current.append('\n');
                current.append(line);
            }
        }
        if (!current.isEmpty() || sections.isEmpty()) {
            sections.add(new Section(heading, level, current.toString().trim(), null));
        }
        return new ParsedDocument(sections, text);
    }
}
