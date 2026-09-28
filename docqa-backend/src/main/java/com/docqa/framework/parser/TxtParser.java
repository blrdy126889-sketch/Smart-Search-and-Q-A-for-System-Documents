package com.docqa.framework.parser;

import com.docqa.framework.parser.DocumentParser.ParsedDocument;
import com.docqa.framework.parser.DocumentParser.Parser;
import com.docqa.framework.parser.DocumentParser.Section;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TXT 解析器：自动探测编码（UTF-8/GBK），识别 markdown / 中式编号标题分节
 */
@Component
public class TxtParser implements Parser {

    private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.+)$|^(第[一二三四五六七八九十百]+[章节][、\\s].*)$");

    @Override
    public boolean supports(String sourceType) {
        return "TXT".equalsIgnoreCase(sourceType);
    }

    @Override
    public ParsedDocument parse(InputStream in) throws Exception {
        byte[] bytes = in.readAllBytes();
        String text = decode(bytes);
        String[] lines = text.split("\\R");
        List<Section> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String heading = "";
        int level = 1;

        for (String line : lines) {
            Matcher m = HEADING.matcher(line.trim());
            if (m.matches()) {
                if (!current.isEmpty()) {
                    sections.add(new Section(heading, level, current.toString().trim(), null));
                    current = new StringBuilder();
                }
                heading = m.group(2) != null ? m.group(2).trim() : m.group(3).trim();
                level = m.group(1) != null ? m.group(1).length() : 2;
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

    private String decode(byte[] bytes) {
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (!utf8.contains("�")) return utf8;
        return new String(bytes, Charset.forName("GBK"));
    }
}
