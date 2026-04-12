package com.siceb.platform.iam.repository;

import com.siceb.platform.iam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.roleId = :roleId AND u.active = true")
    List<User> findByRoleIdAndActive(@Param("roleId") UUID roleId);

    @Query("SELECT u FROM User u JOIN u.assignedBranches b WHERE b.branchId = :branchId AND u.active = true")
    List<User> findByBranchIdAndActive(@Param("branchId") UUID branchId);

    List<User> findByActiveTrue();
}
