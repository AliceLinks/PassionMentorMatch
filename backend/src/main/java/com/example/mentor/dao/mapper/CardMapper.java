package com.example.mentor.dao.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import com.example.mentor.dao.entity.Card;

@Mapper
public interface CardMapper extends BaseMapper<Card> {
}
