package com.example.portal_oportunidades_back.auth.repository;
import com.example.portal_oportunidades_back.auth.entity.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AdministratorRepository extends JpaRepository<Administrator, Long> { }
