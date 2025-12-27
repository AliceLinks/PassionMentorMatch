package com.example.mentor.dao.mapper;

import com.example.mentor.dao.entity.SimpleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;

@Mapper
public interface SimpleConfigMapper {
    @Select("SELECT * FROM simple_config WHERE config_key = #{configKey} LIMIT 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "configKey", column = "config_key"),
        @Result(property = "configValue", column = "config_value"),
        @Result(property = "description", column = "description")
    })
    SimpleConfig selectByKey(@Param("configKey") String configKey);

    @Update("UPDATE simple_config SET config_value = #{configValue} WHERE config_key = #{configKey}")
    int updateValueByKey(@Param("configKey") String configKey, @Param("configValue") String configValue);

    @Insert("INSERT INTO simple_config (config_key, config_value, description) VALUES (#{configKey}, #{configValue}, #{description})")
    int insert(SimpleConfig config);
}