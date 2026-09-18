package com.campus.lab.repository;

import com.campus.lab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    long countByActiveTrue();

    List<User> findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String name, String username);
}
