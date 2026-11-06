package com.apartment.rental.account.repository;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepositoryForM04 extends JpaRepository<User, Long> {

    List<User> findAllByRoleOrderByFullNameAsc(Role role);
}