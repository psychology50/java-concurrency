package com.test.concurrent.repository;

import com.test.concurrent.domain.OptimisticCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OptimisticCouponRepository extends JpaRepository<OptimisticCoupon, Long> {
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM OptimisticCoupon c WHERE c.id = :id")
    Optional<OptimisticCoupon> findByIdWithOLock(Long id);
}
