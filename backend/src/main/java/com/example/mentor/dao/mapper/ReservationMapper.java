package com.example.mentor.dao.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mentor.dao.entity.Reservation;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
