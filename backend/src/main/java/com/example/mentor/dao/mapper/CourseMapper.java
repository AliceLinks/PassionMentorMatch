package com.example.mentor.dao.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mentor.dao.entity.Course;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
