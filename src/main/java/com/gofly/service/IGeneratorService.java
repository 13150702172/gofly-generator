package com.gofly.service;

import com.gofly.entity.Column;
import com.gofly.entity.Table;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IGeneratorService {
    List<String> getDatabases();
    List<Table> getTables(String schemaName, String tableName);
    List<Column> getColumns(String schemaName, String tableName);
}
