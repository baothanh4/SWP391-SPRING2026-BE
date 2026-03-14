package com.example.SWP391_SPRING2026.Repository.Specification;

import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.UserRole;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    private UserSpecifications() {}



    public static Specification<Users> search(String keyword) {

        return (root, query, cb) -> {

            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern)
            );
        };
    }

    public static Specification<Users> hasRole(UserRole role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<Users> hasStatus(UserStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
