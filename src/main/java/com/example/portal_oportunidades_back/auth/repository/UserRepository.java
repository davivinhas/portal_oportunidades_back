package com.example.portal_oportunidades_back.auth.repository;

import com.example.portal_oportunidades_back.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> { }
