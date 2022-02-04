package com.gofly.helper;

import com.gofly.GoflyGeneratorApplication;
import com.gofly.entity.Column;
import com.gofly.entity.FieldType;
import com.gofly.entity.GeneratorConfig;
import com.gofly.entity.GeneratorConstant;
import com.google.common.io.Files;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class GeneratorHelper {

    /**
     * 生成实体类
     * @param columns 列参数
     * @param configure 生成配置
     */
    public void generateEntityFile(List<Column> columns, GeneratorConfig configure) throws Exception {
        //获取java文件后缀
        String javaFileSuffix = GeneratorConstant.JAVA_FILE_SUFFIX;
        //获取生成文件路径
        String filePath = getFilePath(configure, configure.getEntityPackage(), javaFileSuffix, false);
        //实体类freemark模板文件名称
        String entityTemplate = GeneratorConstant.ENTITY_TEMPLATE;
        //获取文件
        File entityFile = new File(filePath);
        //遍历所有参数
        columns.forEach(c->{
            c.setField(underscoreToCamel(StringUtils.lowerCase(c.getName())));
            if (StringUtils.containsAny(c.getType(), FieldType.DATE, FieldType.DATETIME, FieldType.TIMESTAMP)) {
                configure.setHasDate(true);
            }
            if (StringUtils.containsAny(c.getType(), FieldType.DECIMAL, FieldType.NUMERIC)) {
                configure.setHasBigDecimal(true);
            }
        });
        configure.setColumns(columns);
        //生成文件
        generateFileByTemplate(entityTemplate,entityFile,configure);
    }

    /**
     * 生成mapper文件
     * @param configure
     */
    public void generateMapperFile(GeneratorConfig configure) throws Exception {
        String mapperFileSuffix = GeneratorConstant.MAPPER_FILE_SUFFIX;
        String filePath = getFilePath(configure, configure.getMapperPackage(), mapperFileSuffix, false);
        String mapperTemplate = GeneratorConstant.MAPPER_TEMPLATE;
        File file = new File(filePath);
        generateFileByTemplate(mapperTemplate,file,configure);
    }

    /**
     * 生成service接口
     * @param columns
     * @param configure
     * @throws Exception
     */
    public void generateServiceFile(List<Column> columns, GeneratorConfig configure) throws Exception {
        String suffix = GeneratorConstant.SERVICE_FILE_SUFFIX;
        String path = getFilePath(configure, configure.getServicePackage(), suffix, true);
        String templateName = GeneratorConstant.SERVICE_TEMPLATE;
        File serviceFile = new File(path);
        generateFileByTemplate(templateName, serviceFile, configure);
    }

    /**
     * 生成service实现类
     * @param columns
     * @param configure
     * @throws Exception
     */
    public void generateServiceImplFile(List<Column> columns, GeneratorConfig configure) throws Exception {
        String suffix = GeneratorConstant.SERVICEIMPL_FILE_SUFFIX;
        String path = getFilePath(configure, configure.getServiceImplPackage(), suffix, false);
        String templateName = GeneratorConstant.SERVICEIMPL_TEMPLATE;
        File serviceImplFile = new File(path);
        generateFileByTemplate(templateName, serviceImplFile, configure);
    }

    /**
     * 生成controller
     * @param columns
     * @param configure
     * @throws Exception
     */
    public void generateControllerFile(List<Column> columns, GeneratorConfig configure) throws Exception {
        String suffix = GeneratorConstant.CONTROLLER_FILE_SUFFIX;
        String path = getFilePath(configure, configure.getControllerPackage(), suffix, false);
        String templateName = GeneratorConstant.CONTROLLER_TEMPLATE;
        File controllerFile = new File(path);
        generateFileByTemplate(templateName, controllerFile, configure);
    }

    /**
     * 生成mapper xml
     * @param columns
     * @param configure
     * @throws Exception
     */
    public void generateMapperXmlFile(List<Column> columns, GeneratorConfig configure) throws Exception {
        String suffix = GeneratorConstant.MAPPERXML_FILE_SUFFIX;
        String path = getFilePath(configure, configure.getMapperXmlPackage(), suffix, false);
        String templateName = GeneratorConstant.MAPPERXML_TEMPLATE;
        File mapperXmlFile = new File(path);
        columns.forEach(c -> c.setField(underscoreToCamel(StringUtils.lowerCase(c.getName()))));
        configure.setColumns(columns);
        generateFileByTemplate(templateName, mapperXmlFile, configure);
    }


    /**
     * 生成所有的文件
     * @param columns
     * @param config
     * @throws Exception
     */
    public void generateCodeFile(List<Column> columns,GeneratorConfig config) throws Exception {
        generateEntityFile(columns, config);
        generateMapperFile(config);
        generateControllerFile(columns, config);
        generateMapperXmlFile(columns, config);
        generateServiceFile(columns, config);
        generateServiceImplFile(columns, config);
    }


    /******************工具类**************************/

    /**
     * 通过模板生成文件
     * @param templateName
     * @param file
     * @param data
     * @throws Exception
     */
    private void generateFileByTemplate(String templateName, File file, Object data) throws Exception {
        Template template = getTemplate(templateName);
        //创建文件目录
        Files.createParentDirs(file);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try (Writer out = new BufferedWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8), 1024)) {
            template.process(data, out);
        } catch (Exception e) {
            String message = "代码生成异常";
            log.error(message, e);
            throw new Exception(message);
        }
    }

    /**
     * 获取模板
     * @param templateName
     * @return
     * @throws Exception
     */
    private Template getTemplate(String templateName) throws Exception {
        //配置模板版本号
        Configuration configuration = new freemarker.template.Configuration(Configuration.VERSION_2_3_23);
        //获取模板路径
        String templatePath = GeneratorHelper.class.getResource("/generator/templates/").getPath();
        //获取文件
        File file = new File(templatePath);
        if (!file.exists()) {
            templatePath = System.getProperties().getProperty("java.io.tmpdir");
            file = new File(templatePath + "/" + templateName);
            FileUtils.copyInputStreamToFile(Objects.requireNonNull(GoflyGeneratorApplication.class.getClassLoader().getResourceAsStream("classpath:generator/templates/" + templateName)), file);
        }
        configuration.setDirectoryForTemplateLoading(new File(templatePath));
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
        return configuration.getTemplate(templateName);
    }

    /**
     * 下划线转驼峰
     *
     * @param value 待转换值
     * @return 结果
     */
    public static String underscoreToCamel(String value) {
        StringBuilder result = new StringBuilder();
        String[] arr = value.split("_");
        for (String s : arr) {
            result.append((String.valueOf(s.charAt(0))).toUpperCase()).append(s.substring(1));
        }
        return result.toString();
    }

    /**
     * 获取文件路径
     * @param configure 生成配置文件
     * @param packagePath 包路径，比如mapper、entity、controller……
     * @param suffix 后缀
     * @param serviceInterface 是否为接口
     * @return
     */
    private static String getFilePath(GeneratorConfig configure, String packagePath, String suffix, boolean serviceInterface) {
        //获取生成代码的存储文件路径
        String filePath = GeneratorConstant.TEMP_PATH + configure.getJavaPath() +
                packageConvertPath(configure.getBasePackage() + "." + packagePath);
        //如果serviceInterface为true,那么表示为接口
        if (serviceInterface) {
            filePath += "I";
        }
        filePath += configure.getClassName() + suffix;
        return filePath;
    }

    /**
     * 包路径转换
     * TODO: 比如将com.gofly.mapper 转换为com/gofly/mapper
     * @param packageName
     * @return
     */
    private static String packageConvertPath(String packageName) {
        return String.format("/%s/", packageName.contains(".") ? packageName.replaceAll("\\.", "/") : packageName);
    }



}
