package com.moa.api.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.SignUp;

public interface SignUpRepository extends JpaRepository<SignUp, Long> {
}
