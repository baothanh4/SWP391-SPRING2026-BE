package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address,Long> {
    List<Address> findByUserId(Long userId);
    void deleteByIdAndUserId(Long id,Long userId);
    Optional<Address> findByIdAndUser_Id(Long id, Long userId);
    Optional<Address> findFirstByUser_IdAndIsDefaultTrue(Long userId);
}
