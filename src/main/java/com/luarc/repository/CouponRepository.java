package com.luarc.repository;

import com.luarc.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Find coupon by code with pessimistic write lock
     * This ensures only one thread can modify it at a time
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeWithLock(@Param("code") String code);

    /**
     * Find coupon by code without lock (for read operations)
     */
    Optional<Coupon> findByCode(String code);

    /**
     * Find all active coupons that haven't expired
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND c.expiryDate > :now")
    List<Coupon> findActiveAndNotExpired(@Param("now") LocalDateTime now);

    /**
     * Find coupons with available quantity
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
           "AND c.claimedQuantity < c.totalQuantity " +
           "AND c.expiryDate > :now")
    List<Coupon> findAvailableCoupons(@Param("now") LocalDateTime now);

    /**
     * Find coupons expiring soon (within 7 days)
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
           "AND c.expiryDate BETWEEN :now AND :sevenDaysLater")
    List<Coupon> findExpiringCoupons(@Param("now") LocalDateTime now, 
                                      @Param("sevenDaysLater") LocalDateTime sevenDaysLater);
}
