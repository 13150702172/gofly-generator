> 前言
> 

为什么需要代码生成？

我们在做项目的时候，好多时候我们都是需要写重复的代码，比如：

- Controller
- Service
- Mapper
- Entity

等等

所以，我们需要创建代码生成相关的工具类，每一次就无需重新写重复的代码

> 代码生成依赖准备
> 
- Mybatis
- Mysql
- Freemarker
- 一些工具类

```xml
       <!--freemarker-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-freemarker</artifactId>
        </dependency>

        <!--mybatis-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.4.2</version>
        </dependency>

        <!--mysql-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!--读取配置文件-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
        </dependency>

        <!--commons工具类-->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.8.0</version>
        </dependency>

        <!--guava-->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>30.1-jre</version>
        </dependency>

       <!--lombok-->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
```

> 思路
> 

我们要实现代码生成，首先需要理解的是基本流程，代码生成主要参考FEBS项目

- **目录结构：**

```xml
|____src
| |____main
| | |____resources
| | | |____generator.sql
| | | |____mapper
| | | | |____GeneratorMapper.xml   -------------> 获取数据库相关信息
| | | |____generator
| | | | |____templates             -------------> 模板
| | | | | |____mapperXml.ftl
| | | | | |____serviceImpl.ftl
| | | | | |____service.ftl
| | | | | |____controller.ftl
| | | | | |____entity.ftl
| | | | | |____mapper.ftl
| | | |____application.yml
| | |____java
| | | |____com
| | | | |____gofly
| | | | | |____entity   ---------------> 实体对象
| | | | | | |____Table.java
| | | | | | |____GeneratorConstant.java   --------------> 常量
| | | | | | |____FieldType.java           --------------> 字段类型
| | | | | | |____Column.java
| | | | | | |____GeneratorConfig.java     --------------> 文件生成配置
| | | | | |____mapper
| | | | | | |____GeneratorConfigMapper.java
| | | | | | |____GeneratorMapper.java
| | | | | |____utils    ---------------------> 工具类
| | | | | | |____FileUtil.java
| | | | | |____controller
| | | | | | |____GeneratorController.java
| | | | | |____service
| | | | | | |____impl
| | | | | | | |____GeneratorServiceImpl.java
| | | | | | | |____GeneratorConfigServiceImpl.java
| | | | | | |____IGeneratorConfig.java
| | | | | | |____IGeneratorService.java
| | | | | |____helper
| | | | | | |____GeneratorHelper.java   ----------------> 代码生成类
| | | | | |____GoflyGeneratorApplication.java
```

- **基本流程**

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/5e5d597e-c211-4f99-bbaf-453751edf888/Untitled.png)

[代码生成.xmind](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/d1398e6c-2bbf-43c3-adbf-b23685e8c068/代码生成.xmind)

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/11ebc302-b315-472f-ac71-6dff5cde9c7d/Untitled.png)

根据关系图，我们的基本内容如下：

- Service、Mapper方法都是从数据库中获取信息

  `GeneratorMapper`功能是获取数据库、数据表等相关信息

 `GeneratorConfigMapper`功能是获取代码生成的配置信息

`Service方法同上`

- `Table`类，主要是映射数据库表的相关信息，数据来源于`GeneratorMapper`
- `Column`类，主要是映射数据库表中每一个字段的信息，数据来源于`GeneratorMapper`
- `FieldType`类，主要是常量类，存储字段类型
- `GeneratorConstant`类，主要是常量类，存储代码生成相关的相关常量
- `GeneratorConfig`类，主要是存储代码生成相关配置，数据来源于`GeneratorConfigMapper`
- `GeneratorHelper`类，代码生成的核心方法，主要是通过模板生成相应的代码

流程如下：

- **第一步：**从数据库中获取数据库相关信息

`GeneratorMapper`获取相应的信息，这个并不是从某个数据库表中获取信息，而是获取数据库、数据表构成信息，用文字描述起来有点不好理解，还是具体看代码：

```xml
    <!--查询可用数据库-->
    <select id="getDatabases" resultType="java.lang.String">
        SELECT DISTINCT TABLE_SCHEMA FROM information_schema.TABLES
    </select>

   <!--查询数据库下面的表-->
    <select id="getTables" parameterType="string" resultType="com.gofly.entity.Table">
        SELECT
        CREATE_TIME createTime,
        UPDATE_TIME updateTime,
        TABLE_ROWS dataRows,
        TABLE_NAME name,
        TABLE_COMMENT remark
        FROM
        information_schema.TABLES
        WHERE
        TABLE_SCHEMA = #{schemaName}
        <if test="tableName != null and tableName != ''">
            AND TABLE_NAME = #{tableName}
        </if>
    </select>

    <!--查询数据库表下面的列属性-->
    <select id="getColumns" resultType="com.gofly.entity.Column">
        SELECT
        COLUMN_NAME name,
        CASE
            COLUMN_key
        WHEN 'PRI' THEN
                1 ELSE 0
        END isKey,
        DATA_TYPE type,
        COLUMN_COMMENT remark
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = #{schemaName} AND TABLE_NAME = #{tableName}
    </select>
```

`GeneratorConfigMapper`获取代码生成相应的配置，映射为`GeneratorConfig`类

```sql
DROP TABLE IF EXISTS `t_generator_config`;
CREATE TABLE `t_generator_config`  (
                                       `id` int(11) NOT NULL COMMENT '主键',
                                       `author` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '作者',
                                       `base_package` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '基础包名',
                                       `entity_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'entity文件存放路径',
                                       `mapper_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'mapper文件存放路径',
                                       `mapper_xml_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'mapper xml文件存放路径',
                                       `service_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'servcie文件存放路径',
                                       `service_impl_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'serviceImpl文件存放路径',
                                       `controller_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'controller文件存放路径',
                                       `is_trim` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '是否去除前缀 1是 0否',
                                       `trim_value` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '前缀内容',
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '代码生成配置表' ROW_FORMAT = Dynamic;

	-- ----------------------------
-- Records of t_generator_config
-- ----------------------------
INSERT INTO `t_generator_config` VALUES (1, 'Gofly', 'com.gofly', 'entity', 'mapper', 'mapperXml', 'service', 'service.impl', 'controller', '1', 't_');
```

- 第二步：通过Freemark模板生成相关文件，最主要就是`GeneratorHelper`类

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/51b32790-d3ea-4aa3-ac26-2b803376dc14/Untitled.png)

其实，最主要的流程只有两步，只是具体的实现需要动一些脑筋，正式开始Coding

> Coding
> 

<aside>
⚠️ 配置文件yml

</aside>

```yaml
**mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:/mapper/*.xml

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/gofly?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=true
    username: root
    password: yang19960127**
```

<aside>
⚠️ 创建sql文件：代码生成配置文件

</aside>

```sql
DROP TABLE IF EXISTS `t_generator_config`;
CREATE TABLE `t_generator_config`  (
                                       `id` int(11) NOT NULL COMMENT '主键',
                                       `author` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '作者',
                                       `base_package` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '基础包名',
                                       `entity_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'entity文件存放路径',
                                       `mapper_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'mapper文件存放路径',
                                       `mapper_xml_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'mapper xml文件存放路径',
                                       `service_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'servcie文件存放路径',
                                       `service_impl_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'serviceImpl文件存放路径',
                                       `controller_package` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'controller文件存放路径',
                                       `is_trim` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '是否去除前缀 1是 0否',
                                       `trim_value` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '前缀内容',
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '代码生成配置表' ROW_FORMAT = Dynamic;

	-- ----------------------------
-- Records of t_generator_config
-- ----------------------------
INSERT INTO `t_generator_config` VALUES (1, 'Gofly', 'com.gofly', 'entity', 'mapper', 'mapperXml', 'service', 'service.impl', 'controller', '1', 't_');
```

<aside>
⚠️ 创建模板文件

</aside>

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/2dcd38c0-ecf3-451e-ad94-e8bdb4a56fd9/Untitled.png)

- controller.ftl

```java
**package ${basePackage}.${controllerPackage};

import ${basePackage}.${entityPackage}.${className};
import ${basePackage}.${servicePackage}.I${className}Service;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
* ${tableComment} Controller
*
* @author ${author}
* @date ${date}
*/
@Slf4j
@Validated
@Controller
public class ${className}Controller extends BaseController {

private final I${className}Service ${className?uncap_first}Service;

@GetMapping("${className?uncap_first}")
public String ${className?uncap_first}Index(){
return FebsUtil.view("${className?uncap_first}/${className?uncap_first}");
}

@GetMapping("${className?uncap_first}")
@ResponseBody
public FebsResponse getAll${className}s(${className} ${className?uncap_first}) {
return new FebsResponse().success().data(${className?uncap_first}Service.find${className}s(${className?uncap_first}));
}

@GetMapping("${className?uncap_first}/list")
@ResponseBody
@RequiresPermissions("${className?uncap_first}:list")
public FebsResponse ${className?uncap_first}List(QueryRequest request, ${className} ${className?uncap_first}) {
Map<String, Object> dataTable = getDataTable(this.${className?uncap_first}Service.find${className}s(request, ${className?uncap_first}));
return new FebsResponse().success().data(dataTable);
}

@ControllerEndpoint(operation = "新增${className}", exceptionMessage = "新增${className}失败")
@PostMapping("${className?uncap_first}")
@ResponseBody
@RequiresPermissions("${className?uncap_first}:add")
public FebsResponse add${className}(@Valid ${className} ${className?uncap_first}) {
this.${className?uncap_first}Service.create${className}(${className?uncap_first});
return new FebsResponse().success();
}

@ControllerEndpoint(operation = "删除${className}", exceptionMessage = "删除${className}失败")
@GetMapping("${className?uncap_first}/delete")
@ResponseBody
@RequiresPermissions("${className?uncap_first}:delete")
public FebsResponse delete${className}(${className} ${className?uncap_first}) {
this.${className?uncap_first}Service.delete${className}(${className?uncap_first});
return new FebsResponse().success();
}

@ControllerEndpoint(operation = "修改${className}", exceptionMessage = "修改${className}失败")
@PostMapping("${className?uncap_first}/update")
@ResponseBody
@RequiresPermissions("${className?uncap_first}:update")
public FebsResponse update${className}(${className} ${className?uncap_first}) {
this.${className?uncap_first}Service.update${className}(${className?uncap_first});
return new FebsResponse().success();
}

@ControllerEndpoint(operation = "修改${className}", exceptionMessage = "导出Excel失败")
@PostMapping("${className?uncap_first}/excel")
@ResponseBody
@RequiresPermissions("${className?uncap_first}:export")
public void export(QueryRequest queryRequest, ${className} ${className?uncap_first}, HttpServletResponse response) {
List<${className}> ${className?uncap_first}s = this.${className?uncap_first}Service.find${className}s(queryRequest, ${className?uncap_first}).getRecords();
ExcelKit.$Export(${className}.class, response).downXlsx(${className?uncap_first}s, false);
}
}**
```

- entity.ftl

```java
**package ${basePackage}.${entityPackage};

<#if hasDate = true>
    import java.util.Date;
</#if>
<#if hasBigDecimal = true>
    import java.math.BigDecimal;
</#if>

import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
* ${tableComment} Entity
*
* @author ${author}
* @date ${date}
*/
@Data
@TableName("${tableName}")
public class ${className} {

<#if columns??>
    <#list columns as column>
        /**
        * ${column.remark}
        */
        <#if column.isKey = true>
            @TableId(value = "${column.name}", type = IdType.AUTO)
        <#else>
            @TableField("${column.name}")
        </#if>
        <#if (column.type = 'varchar' || column.type = 'text' || column.type = 'uniqueidentifier'
        || column.type = 'varchar2' || column.type = 'nvarchar' || column.type = 'VARCHAR2'
        || column.type = 'VARCHAR'|| column.type = 'CLOB' || column.type = 'char')>
            private String ${column.field?uncap_first};

        </#if>
        <#if column.type = 'timestamp' || column.type = 'date' || column.type = 'datetime'||column.type = 'TIMESTAMP' || column.type = 'DATE' || column.type = 'DATETIME'>
            private Date ${column.field?uncap_first};

        </#if>
        <#if column.type = 'int' || column.type = 'smallint'>
            private Integer ${column.field?uncap_first};

        </#if>
        <#if column.type = 'bigint'>
            private Long ${column.field?uncap_first};

        </#if>
        <#if column.type = 'double'>
            private Double ${column.field?uncap_first};

        </#if>
        <#if column.type = 'tinyint'>
            private Byte ${column.field?uncap_first};

        </#if>
        <#if column.type = 'decimal' || column.type = 'numeric'>
            private BigDecimal ${column.field?uncap_first};
        </#if>
    </#list>
</#if>
}**
```

- mapper.ftl

```java
**package ${basePackage}.${mapperPackage};

import ${basePackage}.${entityPackage}.${className};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* ${tableComment} Mapper
*
* @author ${author}
* @date ${date}
*/
public interface ${className}Mapper extends BaseMapper<${className}> {

}**
```

- mapperXml.ftl

```java
**<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${basePackage}.${mapperPackage}.${className}Mapper">

</mapper>**
```

- service.ftl

```java
**package ${basePackage}.${servicePackage};

import ${basePackage}.${entityPackage}.${className};

import cc.mrbird.febs.common.entity.QueryRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* ${tableComment} Service接口
*
* @author ${author}
* @date ${date}
*/
public interface I${className}Service extends IService<${className}> {
/**
* 查询（分页）
*
* @param request QueryRequest
* @param ${className?uncap_first} ${className?uncap_first}
* @return IPage<${className}>
*/
IPage<${className}> find${className}s(QueryRequest request, ${className} ${className?uncap_first});

/**
* 查询（所有）
*
* @param ${className?uncap_first} ${className?uncap_first}
* @return List<${className}>
*/
List<${className}> find${className}s(${className} ${className?uncap_first});

/**
* 新增
*
* @param ${className?uncap_first} ${className?uncap_first}
*/
void create${className}(${className} ${className?uncap_first});

/**
* 修改
*
* @param ${className?uncap_first} ${className?uncap_first}
*/
void update${className}(${className} ${className?uncap_first});

/**
* 删除
*
* @param ${className?uncap_first} ${className?uncap_first}
*/
void delete${className}(${className} ${className?uncap_first});
}**
```

- serviceImpl.ftl

```java
**package ${basePackage}.${serviceImplPackage};

import cc.mrbird.febs.common.entity.QueryRequest;
import ${basePackage}.${entityPackage}.${className};
import ${basePackage}.${mapperPackage}.${className}Mapper;
import ${basePackage}.${servicePackage}.I${className}Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
* ${tableComment} Service实现
*
* @author ${author}
* @date ${date}
*/
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ${className}ServiceImpl extends ServiceImpl<${className}Mapper, ${className}> implements I${className}Service {

private final ${className}Mapper ${className?uncap_first}Mapper;

@Override
public IPage<${className}> find${className}s(QueryRequest request, ${className} ${className?uncap_first}) {
LambdaQueryWrapper<${className}> queryWrapper = new LambdaQueryWrapper<>();
// TODO 设置查询条件
Page<${className}> page = new Page<>(request.getPageNum(), request.getPageSize());
return this.page(page, queryWrapper);
}

@Override
public List<${className}> find${className}s(${className} ${className?uncap_first}) {
LambdaQueryWrapper<${className}> queryWrapper = new LambdaQueryWrapper<>();
// TODO 设置查询条件
return this.baseMapper.selectList(queryWrapper);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void create${className}(${className} ${className?uncap_first}) {
this.save(${className?uncap_first});
}

@Override
@Transactional(rollbackFor = Exception.class)
public void update${className}(${className} ${className?uncap_first}) {
this.saveOrUpdate(${className?uncap_first});
}

@Override
@Transactional(rollbackFor = Exception.class)
public void delete${className}(${className} ${className?uncap_first}) {
LambdaQueryWrapper<${className}> wrapper = new LambdaQueryWrapper<>();
// TODO 设置删除条件
this.remove(wrapper);
}
}**
```

<aside>
⚠️ 创建实体类和常量类

</aside>

- `Column`

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/4f59aefc-5112-4625-8fe2-0cc713e4211a/Untitled.png)

```java
package com.gofly.entity;

import lombok.Data;

@Data
public class Column {
    /**
     * 名称
     */
    private String name;
    /**
     * 是否为主键
     */
    private Boolean isKey;
    /**
     * 类型
     */
    private String type;
    /**
     * 注释
     */
    private String remark;
    /**
     * 属性名称
     */
    private String field;
}
```

- `FieldType`

```java
package com.gofly.entity;

/**
 * 字段类型
 */
public interface FieldType {
    public static final String DATE = "date";
    public static final String DATETIME = "datetime";
    public static final String TIMESTAMP = "timestamp";
    public static final String DECIMAL = "decimal";
    public static final String NUMERIC = "numeric";
}
```

- `GeneratorConfig`

```java
package com.gofly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("t_generator_config")
public class GeneratorConfig {
    public static final String TRIM_YES = "1";
    public static final String TRIM_NO = "0";

    /**
     * 主键
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private String id;

    /**
     * 作者
     */
    @TableField("author")
    private String author;

    /**
     * 基础包名
     */
    @TableField("base_package")
    private String basePackage;

    /**
     * entity文件存放路径
     */
    @TableField("entity_package")
    private String entityPackage;

    /**
     * mapper文件存放路径
     */
    @TableField("mapper_package")
    private String mapperPackage;

    /**
     * mapper xml文件存放路径
     */
    @TableField("mapper_xml_package")
    private String mapperXmlPackage;

    /**
     * servcie文件存放路径
     */
    @TableField("service_package")
    private String servicePackage;

    /**
     * serviceImpl文件存放路径
     */
    @TableField("service_impl_package")
    private String serviceImplPackage;

    /**
     * controller文件存放路径
     */
    @TableField("controller_package")
    private String controllerPackage;

    /**
     * 是否去除前缀
     */
    @TableField("is_trim")
    private String isTrim;

    /**
     * 前缀内容
     */
    @TableField("trim_value")
    private String trimValue;

    /**
     * java文件路径，固定值
     */
    private transient String javaPath = "src/main/java";
    /**
     * 配置文件存放路径，固定值
     */
    private transient String resourcesPath = "src/main/resources";
    /**
     * 文件生成日期
     */
    private transient String date = new Date().toString();

    /**
     * 表名
     */
    private transient String tableName;
    /**
     * 表注释
     */
    private transient String tableComment;
    /**
     * 数据表对应的类名
     */
    private transient String className;

    private transient boolean hasDate;
    private transient boolean hasBigDecimal;
    private transient List<Column> columns;
}
```

- `GeneratorConstant`

```java
package com.gofly.entity;

public interface GeneratorConstant {
    /**
     * 数据库类型
     */
    String DATABASE_TYPE = "mysql";

    /**
     * 生成代码的临时目录
     */
    String TEMP_PATH = "temp/";

    /**
     * java类型文件后缀
     */
    String JAVA_FILE_SUFFIX = ".java";
    /**
     * mapper文件类型后缀
     */
    String MAPPER_FILE_SUFFIX = "Mapper.java";
    /**
     * service文件类型后缀
     */
    String SERVICE_FILE_SUFFIX = "Service.java";
    /**
     * service impl文件类型后缀
     */
    String SERVICEIMPL_FILE_SUFFIX = "ServiceImpl.java";
    /**
     * controller文件类型后缀
     */
    String CONTROLLER_FILE_SUFFIX = "Controller.java";
    /**
     * mapper xml文件类型后缀
     */
    String MAPPERXML_FILE_SUFFIX = "Mapper.xml";
    /**
     * entity模板
     */
    String ENTITY_TEMPLATE = "entity.ftl";
    /**
     * mapper模板
     */
    String MAPPER_TEMPLATE = "mapper.ftl";
    /**
     * service接口模板
     */
    String SERVICE_TEMPLATE = "service.ftl";
    /**
     * service impl接口模板
     */
    String SERVICEIMPL_TEMPLATE = "serviceImpl.ftl";
    /**
     * controller接口模板
     */
    String CONTROLLER_TEMPLATE = "controller.ftl";
    /**
     * mapper xml接口模板
     */
    String MAPPERXML_TEMPLATE = "mapperXml.ftl";
}
```

- `Table`

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/5e87e3fb-18bd-45be-808a-66c9771a868d/Untitled.png)

```java
package com.gofly.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Table {
    private Date createTime;
    private Date updateTime;
    private String dataRows;
    private String name;
    private String remark;
}
```

<aside>
⚠️ 创建Mapper和Mapper xml

</aside>

- 创建`GeneratorMapper.java`

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/4dcace60-509f-41e3-a63b-08dfd4d3352b/Untitled.png)

```java
**package com.gofly.mapper;

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
}**
```

- 创建`GeneratorMapper.xml`

```xml
**<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.gofly.mapper.GeneratorMapper">
    <!--查询可用数据库-->
    <select id="getDatabases" resultType="java.lang.String">
        SELECT DISTINCT TABLE_SCHEMA FROM information_schema.TABLES
    </select>

    <!--查询数据库下面的表-->
    <select id="getTables" parameterType="string" resultType="com.gofly.entity.Table">
        SELECT
        CREATE_TIME createTime,
        UPDATE_TIME updateTime,
        TABLE_ROWS dataRows,
        TABLE_NAME name,
        TABLE_COMMENT remark
        FROM
        information_schema.TABLES
        WHERE
        TABLE_SCHEMA = #{schemaName}
        <if test="tableName != null and tableName != ''">
            AND TABLE_NAME = #{tableName}
        </if>
    </select>

    <!--查询数据库表下面的列属性-->
    <select id="getColumns" resultType="com.gofly.entity.Column">
        SELECT
        COLUMN_NAME name,
        CASE
            COLUMN_key
        WHEN 'PRI' THEN
                1 ELSE 0
        END isKey,
        DATA_TYPE type,
        COLUMN_COMMENT remark
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = #{schemaName} AND TABLE_NAME = #{tableName}
    </select>

</mapper>**
```

- 创建`GeneratorConfigMapper.java`

因为我们继承了`BaseMapper<GeneratorConfig>`，所以我们无需创建mapper.xml文件，因为mybatis已经帮忙封装好了，直接调用接口即可

```java
**package com.gofly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gofly.entity.GeneratorConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GeneratorConfigMapper extends BaseMapper<GeneratorConfig> {
}**
```

<aside>
⚠️ 创建Service接口和实现类

</aside>

- `IGeneratorConfig` 接口

```java
**package com.gofly.service;

import com.gofly.entity.GeneratorConfig;

public interface IGeneratorConfig {
    /**
     * 查询
     *
     * @return GeneratorConfig
     */
    GeneratorConfig findGeneratorConfig();

    /**
     * 修改
     *
     * @param generatorConfig generatorConfig
     */
    void updateGeneratorConfig(GeneratorConfig generatorConfig);
}**
```

- `GeneratorConfigServiceImpl` 实现类

```java
**package com.gofly.service.impl;

import com.gofly.entity.GeneratorConfig;
import com.gofly.mapper.GeneratorConfigMapper;
import com.gofly.service.IGeneratorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class GeneratorConfigServiceImpl implements IGeneratorConfig {

    @Autowired
    private GeneratorConfigMapper generatorConfigMapper;

    @Override
    public GeneratorConfig findGeneratorConfig() {
        List<GeneratorConfig> generatorConfigList = generatorConfigMapper.selectList(null);
        return CollectionUtils.isEmpty(generatorConfigList) ? null : generatorConfigList.get(0) ;
    }

    @Override
    public void updateGeneratorConfig(GeneratorConfig generatorConfig) {
        generatorConfigMapper.updateById(generatorConfig);
    }
}**
```

- `IGeneratorService` 接口

```java
**package com.gofly.service;

import com.gofly.entity.Column;
import com.gofly.entity.Table;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IGeneratorService {
    List<String> getDatabases();
    List<Table> getTables(String schemaName, String tableName);
    List<Column> getColumns(String schemaName, String tableName);
}**
```

- `GeneratorServiceImpl` 实现类

```java
**package com.gofly.service.impl;

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
}**
```

<aside>
⚠️ 工具类：`FileUtil`

</aside>

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/3909aff9-048d-4180-bdff-8a968e709a4c/Untitled.png)

```java
**package com.gofly.utils;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.FileSystemUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class FileUtil {
    private static final int BUFFER = 1024 * 8;

    private static String[] VALID_FILE_TYPE = {"xlsx", "zip"};

    /**
     * 压缩文件或目录
     *
     * @param fromPath 待压缩文件或路径
     * @param toPath   压缩文件，如 xx.zip
     */
    public static void compress(String fromPath, String toPath) throws IOException {
        File fromFile = new File(fromPath);
        File toFile = new File(toPath);
        if (!fromFile.exists()) {
            throw new FileNotFoundException(fromPath + "不存在！");
        }
        try (
                FileOutputStream outputStream = new FileOutputStream(toFile);
                CheckedOutputStream checkedOutputStream = new CheckedOutputStream(outputStream, new CRC32());
                ZipOutputStream zipOutputStream = new ZipOutputStream(checkedOutputStream)
        ) {
            String baseDir = "";
            compress(fromFile, zipOutputStream, baseDir);
        }
    }

    /**
     * 文件下载
     *
     * @param filePath 待下载文件路径
     * @param fileName 下载文件名称
     * @param delete   下载后是否删除源文件
     * @param response HttpServletResponse
     * @throws Exception Exception
     */
    public static void download(String filePath, String fileName, Boolean delete, HttpServletResponse response) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("文件未找到");
        }

        String fileType = getFileType(file);
        if (!fileTypeIsValid(fileType)) {
            throw new Exception("暂不支持该类型文件下载");
        }
        response.setHeader("Content-Disposition", "attachment;fileName=" + java.net.URLEncoder.encode(fileName, "utf-8"));
        response.setContentType("multipart/form-data");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (InputStream inputStream = new FileInputStream(file); OutputStream os = response.getOutputStream()) {
            byte[] b = new byte[2048];
            int length;
            while ((length = inputStream.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } finally {
            if (delete) {
                FileSystemUtils.deleteRecursively(file);
            }
        }
    }

    /**
     * 获取文件类型
     *
     * @param file 文件
     * @return 文件类型
     * @throws Exception Exception
     */
    private static String getFileType(File file) throws Exception {
        Preconditions.checkNotNull(file);
        if (file.isDirectory()) {
            throw new Exception("file不是文件");
        }
        String fileName = file.getName();
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 校验文件类型是否是允许下载的类型
     * （出于安全考虑：https://github.com/wuyouzhuguli/FEBS-Shiro/issues/40）
     *
     * @param fileType fileType
     * @return Boolean
     */
    private static Boolean fileTypeIsValid(String fileType) {
        Preconditions.checkNotNull(fileType);
        fileType = StringUtils.lowerCase(fileType);
        return ArrayUtils.contains(VALID_FILE_TYPE, fileType);
    }

    private static void compress(File file, ZipOutputStream zipOut, String baseDir) throws IOException {
        if (file.isDirectory()) {
            compressDirectory(file, zipOut, baseDir);
        } else {
            compressFile(file, zipOut, baseDir);
        }
    }

    private static void compressDirectory(File dir, ZipOutputStream zipOut, String baseDir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null && ArrayUtils.isNotEmpty(files)) {
            for (File file : files) {
                compress(file, zipOut, baseDir + dir.getName() + File.separator);
            }
        }
    }

    private static void compressFile(File file, ZipOutputStream zipOut, String baseDir) throws IOException {
        if (!file.exists()) {
            return;
        }
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            ZipEntry entry = new ZipEntry(baseDir + file.getName());
            zipOut.putNextEntry(entry);
            int count;
            byte[] data = new byte[BUFFER];
            while ((count = bis.read(data, 0, BUFFER)) != -1) {
                zipOut.write(data, 0, count);
            }
        }
    }
}**
```

<aside>
⚠️ 代码生成类:`GeneratorHelper`

</aside>

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/51b32790-d3ea-4aa3-ac26-2b803376dc14/Untitled.png)

```java
**package com.gofly.helper;

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

}**
```

<aside>
⚠️ 创建Controller

</aside>

```java
**package com.gofly.controller;

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

}**
```
