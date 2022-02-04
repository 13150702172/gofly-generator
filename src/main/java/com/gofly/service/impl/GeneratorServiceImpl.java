package com.gofly.service.impl;

import com.gofly.entity.Column;
import com.gofly.entity.Table;
import com.gofly.mapper.GeneratorMapper;
import com.gofly.service.IGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeneratorServiceImpl implements IGeneratorService {

    @Autowired
    private GeneratorMapper generatorMapper;

    @Override
    public List<String> getDatabases() {
        return generatorMapper.getDatabases();
    }

    @Override
    public List<Table> getTables(String schemaName, String tableName) {
        return generatorMapper.getTables(schemaName, tableName);
    }

    @Override
    public List<Column> getColumns(String schemaName, String tableName) {
        return generatorMapper.getColumns(schemaName, tableName);
    }
}
