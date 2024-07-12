package com.test.concurrent.repository;

import com.test.concurrent.domain.AtomicCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtomicCouponRepository extends JpaRepository<AtomicCoupon, Long> {
}
