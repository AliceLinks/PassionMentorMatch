package com.example.mentor.service;

import com.example.mentor.dao.entity.SimpleConfig;
import com.example.mentor.repository.SimpleConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SimpleConfigService {
    @Autowired
    private SimpleConfigRepository simpleConfigRepository;

    public String getValue(String key) {
        SimpleConfig config = simpleConfigRepository.getByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    public boolean setValue(String key, String value) {
        SimpleConfig config = simpleConfigRepository.getByKey(key);
        if (config == null) {
            config = new SimpleConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription("");
            return simpleConfigRepository.insert(config);
        } else {
            return simpleConfigRepository.updateValueByKey(key, value);
        }
    }
}