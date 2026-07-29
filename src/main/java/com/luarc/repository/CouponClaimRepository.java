package com.luarc.repository;

import com.luarc.entity.CouponClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponClaimRepository extends JpaRepository<CouponClaim, Long> {

    /**
     * Check if a user has already claimed a specific coupon
     */
    @Query("SELECT COUNT(cc) > 0 FROM CouponClaim cc " +
           "WHERE cc.user.id = :userId AND cc.coupon.id = :couponId")
    boolean hasUserClaimedCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * Get all claims for a specific user with coupon details (JOIN)
     */
    @Query("SELECT cc FROM CouponClaim cc " +
           "JOIN FETCH cc.coupon " +
           "WHERE cc.user.id = :userId " +
           "ORDER BY cc.claimedAt DESC")
    List<CouponClaim> findUserClaimsWithCoupons(@Param("userId") Long userId);

    /**
     * Get paginated claims for a user
     */
    @Query("SELECT cc FROM CouponClaim cc " +
           "WHERE cc.user.id = :userId")
    Page<CouponClaim> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Get all claims for a specific coupon
     */
    @Query("SELECT cc FROM CouponClaim cc " +
           "WHERE cc.coupon.id = :couponId " +
           "ORDER BY cc.claimedAt DESC")
    List<CouponClaim> findByCouponId(@Param("couponId") Long couponId);

    /**
     * Get specific claim if exists
     */
    Optional<CouponClaim> findByUserIdAndCouponId(Long userId, Long couponId);
}
