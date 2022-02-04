package com.gofly.mapper;

import com.gofly.entity.Column;
import com.gofly.entity.Table;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GeneratorMapper {
    List<String> getDatabases();
    List<Table> getTables(@Param("schemaName") String schemaName,@Param("tableName") String tableName);
    List<Column> getColumns(@Param("schemaName") String schemaName, @Param("tableName") String tableName);
}
