package com.moa.api.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
