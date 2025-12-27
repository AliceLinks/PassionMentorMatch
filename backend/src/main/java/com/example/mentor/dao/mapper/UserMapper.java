	package com.example.mentor.dao.mapper;

	import com.baomidou.mybatisplus.core.mapper.BaseMapper;
	import com.example.mentor.dao.entity.User;
	import org.apache.ibatis.annotations.Mapper;

	@Mapper
	public interface UserMapper extends BaseMapper<User> {
		@org.apache.ibatis.annotations.Select("SELECT * FROM users WHERE phone = #{phone} LIMIT 1")
		User selectByPhone(String phone);
		// 插入新用户（MyBatis-Plus已支持）
		int insert(User user);
	}