package com.docqa.framework.parser;

import com.docqa.common.exception.BizException;
import com.docqa.framework.parser.DocumentParser.Parser;
import com.docqa.framework.parser.DocumentParser.ParsedDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 解析器工厂：按 sourceType 分发
 */
@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final List<Parser> parsers;

    public ParsedDocument parse(String sourceType, InputStream in) {
        Parser parser = parsers.stream()
                .filter(p -> p.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new BizException("不支持的文档格式：" + sourceType));
        try {
            return parser.parse(in);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("文档解析失败：" + e.getMessage());
        }
    }
}
