package com.docqa.framework.parser;

import com.docqa.framework.parser.DocumentParser.ParsedDocument;
import com.docqa.framework.parser.DocumentParser.Parser;
import com.docqa.framework.parser.DocumentParser.Section;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 解析器：PDFBox 逐页提取（保留页码供来源定位跳转）
 */
@Component
public class PdfParser implements Parser {

    private static final Pattern HEADING = Pattern.compile("^(第[一二三四五六七八九十百]+[章节][、\\s].{0,60})$|^([一二三四五六七八九十]+[、.].{0,50})$");

    @Override
    public boolean supports(String sourceType) {
        return "PDF".equalsIgnoreCase(sourceType);
    }

    @Override
    public ParsedDocument parse(InputStream in) throws Exception {
        List<Section> sections = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        try (PDDocument doc = PDDocument.load(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pages = doc.getNumberOfPages();
            for (int p = 1; p <= pages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(doc);
                fullText.append(pageText);
                splitByHeading(pageText, p, sections);
            }
        }
        if (sections.isEmpty() && !fullText.isEmpty()) {
            sections.add(new Section("", 1, fullText.toString().trim(), 1));
        }
        return new ParsedDocument(sections, fullText.toString());
    }

    private void splitByHeading(String pageText, int pageNo, List<Section> sections) {
        String heading = "";
        int level = 1;
        StringBuilder current = new StringBuilder();
        for (String raw : pageText.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            Matcher m = HEADING.matcher(line);
            if (m.matches()) {
                if (!current.isEmpty()) {
                    sections.add(new Section(heading, level, current.toString().trim(), pageNo));
                    current = new StringBuilder();
                }
                heading = m.group(1) != null ? m.group(1).trim() : m.group(2).trim();
                level = m.group(1) != null ? 2 : 3;
            } else {
                if (!current.isEmpty()) current.append('\n');
                current.append(line);
            }
        }
        if (!current.isEmpty()) {
            sections.add(new Section(heading, level, current.toString().trim(), pageNo));
        }
    }
}
