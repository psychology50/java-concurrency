package com.test.concurrent.repository;

import com.test.concurrent.domain.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithPLock(Long id);

    @Modifying
    @Query("UPDATE Coupon c SET c.availableStock = c.availableStock - :count WHERE c.id = :couponId")
    void decreaseStock(Long couponId, int count);
}
