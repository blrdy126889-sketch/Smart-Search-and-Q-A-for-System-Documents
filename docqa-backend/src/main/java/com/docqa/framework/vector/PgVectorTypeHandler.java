package com.docqa.framework.vector;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL pgvector 类型处理器：Java List<Double> ↔ PG vector
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgVectorTypeHandler extends BaseTypeHandler<List<Double>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Double> parameter, JdbcType jdbcType)
            throws SQLException {
        String vectorStr = parameter.stream()
                .map(v -> String.format("%.6f", v))
                .collect(Collectors.joining(",", "[", "]"));
        ps.setString(i, vectorStr);
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Double> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<Double> parse(String value) {
        if (value == null || value.isEmpty()) return null;
        String s = value.startsWith("[") ? value.substring(1, value.length() - 1) : value;
        List<Double> list = new ArrayList<>();
        for (String part : s.split(",")) {
            if (!part.isBlank()) list.add(Double.parseDouble(part.trim()));
        }
        return list;
    }
}
