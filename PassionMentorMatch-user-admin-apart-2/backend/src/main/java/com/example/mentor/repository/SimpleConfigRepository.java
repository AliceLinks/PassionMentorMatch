package com.example.mentor.repository;

import com.example.mentor.dao.entity.SimpleConfig;
import com.example.mentor.dao.mapper.SimpleConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SimpleConfigRepository {
    @Autowired
    private SimpleConfigMapper simpleConfigMapper;

    public SimpleConfig getByKey(String key) {
        return simpleConfigMapper.selectByKey(key);
    }

    public boolean updateValueByKey(String key, String value) {
        return simpleConfigMapper.updateValueByKey(key, value) > 0;
    }

    public boolean insert(SimpleConfig config) {
        return simpleConfigMapper.insert(config) > 0;
    }
}