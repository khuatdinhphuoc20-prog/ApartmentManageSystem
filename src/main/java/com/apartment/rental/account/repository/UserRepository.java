package com.apartment.rental.account.repository;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findAllByRole(Role role);
}
