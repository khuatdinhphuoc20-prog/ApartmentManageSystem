package com.apartment.rental.account.repository;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleOrderByFullNameAsc(Role role);
}