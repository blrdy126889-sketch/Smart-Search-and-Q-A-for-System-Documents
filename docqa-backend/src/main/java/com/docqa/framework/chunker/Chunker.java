package com.docqa.framework.chunker;

import com.docqa.framework.parser.DocumentParser.ParsedDocument;
import com.docqa.framework.parser.DocumentParser.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归字符切片器：markdown 标题 → 段落 → 句号 → 字符硬切；
 * 不跨章节（携带 heading_path）；滑动窗口重叠；短节合并防碎片
 */
public class Chunker {

    public record TextChunk(String content, String headingPath, Integer pageNo) { }

    private final int chunkSize;
    private final int overlap;

    public Chunker(int chunkSize, int overlap) {
        this.chunkSize = Math.max(chunkSize, 100);
        this.overlap = Math.min(Math.max(overlap, 0), chunkSize / 3);
    }

    public List<TextChunk> chunk(ParsedDocument doc) {
        List<TextChunk> chunks = new ArrayList<>();
        List<Section> sections = mergeShortSections(doc.sections());

        for (Section section : sections) {
            String headingPath = section.heading() == null || section.heading().isEmpty()
                    ? "" : section.heading();
            String text = section.text();
            if (text == null || text.isBlank()) continue;
            if (text.length() <= chunkSize) {
                chunks.add(new TextChunk(text.trim(), headingPath, section.pageNo()));
                continue;
            }
            chunks.addAll(slideWindow(text, headingPath, section.pageNo()));
        }
        return chunks;
    }

    private List<TextChunk> slideWindow(String text, String headingPath, Integer pageNo) {
        List<TextChunk> result = new ArrayList<>();
        List<String> blocks = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String para : text.split("\n")) {
            String p = para.trim();
            if (p.isEmpty()) continue;
            if (buf.length() + p.length() + 1 > chunkSize && buf.length() > 0) {
                blocks.add(buf.toString());
                buf = new StringBuilder();
            }
            if (p.length() > chunkSize) {
                if (buf.length() > 0) { blocks.add(buf.toString()); buf = new StringBuilder(); }
                for (String sentence : splitLong(p)) blocks.add(sentence);
            } else {
                if (buf.length() > 0) buf.append('\n');
                buf.append(p);
            }
        }
        if (buf.length() > 0) blocks.add(buf.toString());

        String prevTail = "";
        for (String block : blocks) {
            String content = prevTail.isEmpty() ? block : prevTail + "\n" + block;
            result.add(new TextChunk(content, headingPath, pageNo));
            prevTail = block.length() > overlap ? block.substring(block.length() - overlap) : block;
        }
        return result;
    }

    private List<String> splitLong(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String sentence : text.split("(?<=[。！？；!?;])")) {
            if (buf.length() + sentence.length() > chunkSize && buf.length() > 0) {
                parts.add(buf.toString());
                buf = new StringBuilder();
            }
            if (sentence.length() > chunkSize) {
                if (buf.length() > 0) { parts.add(buf.toString()); buf = new StringBuilder(); }
                for (int i = 0; i < sentence.length(); i += chunkSize) {
                    parts.add(sentence.substring(i, Math.min(sentence.length(), i + chunkSize)));
                }
            } else {
                buf.append(sentence);
            }
        }
        if (buf.length() > 0) parts.add(buf.toString());
        return parts;
    }

    private List<Section> mergeShortSections(List<Section> sections) {
        List<Section> merged = new ArrayList<>();
        for (Section s : sections) {
            int len = s.text() == null ? 0 : s.text().length();
            if (!merged.isEmpty() && len < 100) {
                Section prev = merged.remove(merged.size() - 1);
                merged.add(new Section(
                        prev.heading(),
                        prev.level(),
                        (prev.text() + "\n" + (s.heading() == null ? "" : s.heading() + "\n") + s.text()).trim(),
                        prev.pageNo()));
            } else {
                merged.add(s);
            }
        }
        return merged;
    }
}
