package com.gofly.service;

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
}
