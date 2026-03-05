package com.aquariux.trading.repository;

import com.aquariux.trading.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
