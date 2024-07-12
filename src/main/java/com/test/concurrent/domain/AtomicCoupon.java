package com.test.concurrent.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AtomicCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    /**
     * 사용 가능 재고수량
     */
    private AtomicLong availableStock;

    public AtomicCoupon(String name, long availableStock) {
        this.name = name;
        this.availableStock = new AtomicLong(availableStock);
    }

    public void decreaseStock() { // 실패하는 코드. 비교와 수정 원자적이지 않음.
        validateStock();
        this.availableStock.decrementAndGet();
    }

    private void validateStock() {
        if (availableStock.get() < 1) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
    }
}
