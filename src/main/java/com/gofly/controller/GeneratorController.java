package com.gofly.controller;

import com.gofly.entity.Column;
import com.gofly.entity.GeneratorConfig;
import com.gofly.entity.GeneratorConstant;
import com.gofly.helper.GeneratorHelper;
import com.gofly.service.IGeneratorConfig;
import com.gofly.service.IGeneratorService;
import com.gofly.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;


@RestController
public class GeneratorController {

    @Autowired
    private IGeneratorService generatorService;

    @Autowired
    private IGeneratorConfig generatorConfig;

    @Autowired
    private GeneratorHelper generatorHelper;

    private static final String SUFFIX = "_code.zip";

    /**
     * 生成文件
     * @param tableName
     * @param remark
     * @param database
     * @param response
     * @throws Exception
     */
    @GetMapping("/generator")
    public void generate(String tableName, String remark, String database, HttpServletResponse response) throws Exception {
        //获取代码生成配置文件
        GeneratorConfig generatorConfig = this.generatorConfig.findGeneratorConfig();
        if(generatorConfig == null){
            throw new RuntimeException("代码生成配置文件为空");
        }

        String className = tableName;
        generatorConfig.setTableName(tableName);
        generatorConfig.setClassName(GeneratorHelper.underscoreToCamel(className));
        generatorConfig.setTableComment(remark);
        List<Column> columns = generatorService.getColumns(database, tableName);
        //生成所有文件
        generatorHelper.generateCodeFile(columns,generatorConfig);
        // 打包
        String zipFile = System.currentTimeMillis() + SUFFIX;
        FileUtil.compress(GeneratorConstant.TEMP_PATH + "src", zipFile);
        // 下载
        FileUtil.download(zipFile, tableName + SUFFIX, true, response);
        // 删除临时目录
        FileSystemUtils.deleteRecursively(new File(GeneratorConstant.TEMP_PATH));
    }


}
