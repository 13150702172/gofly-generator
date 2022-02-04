package com.gofly.service.impl;

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
}
