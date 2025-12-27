package com.example.mentor.repository;

import com.example.mentor.dao.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
}
