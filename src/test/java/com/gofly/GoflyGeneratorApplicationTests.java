package com.gofly;

import com.gofly.entity.Column;
import com.gofly.entity.GeneratorConfig;
import com.gofly.entity.Table;
import com.gofly.helper.GeneratorHelper;
import com.gofly.mapper.GeneratorConfigMapper;
import com.gofly.mapper.GeneratorMapper;
import com.gofly.service.IGeneratorConfig;
import com.gofly.service.IGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class GoflyGeneratorApplicationTests {

    @Autowired
    private GeneratorMapper generatorMapper;

    @Autowired
    private GeneratorConfigMapper generatorConfigMapper;

    @Autowired
    private IGeneratorConfig generatorConfig;


    @Test
    void getDatabasesTest() {
        List<String> databases = generatorMapper.getDatabases();
        System.out.println(databases);
    }

    @Test
    void getTablesTest(){
        List<Table> tables = generatorMapper.getTables("gofly", "t_user");
        System.out.println(tables);
    }

    @Test
    void getColumnsTest(){
        List<Column> columns = generatorMapper.getColumns("gofly", "t_user");
        System.out.println(columns);
    }

    @Test
    void generatorConfigTest(){
//        GeneratorConfig generatorConfig = this.generatorConfig.findGeneratorConfig();
//        System.out.println("generatorConfig:"+generatorConfig);
//        String filePath = GeneratorHelper.getFilePath(generatorConfig, "controller", ".java", false);
//        System.out.println("filePath:"+filePath);
    }


}
