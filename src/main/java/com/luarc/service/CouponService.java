package com.luarc.service;

import com.luarc.dto.CouponDTO;
import com.luarc.entity.Coupon;
import com.luarc.entity.CouponClaim;
import com.luarc.entity.User;
import com.luarc.repository.CouponClaimRepository;
import com.luarc.repository.CouponRepository;
import com.luarc.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new coupon (admin operation)
     */
    @Transactional
    public CouponDTO.Response createCoupon(CouponDTO.CreateRequest request) {
        // Check if coupon code already exists
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists: " + request.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .discountValue(request.getDiscountValue())
                .discountType(request.getDiscountType())
                .totalQuantity(request.getTotalQuantity())
                .claimedQuantity(0)
                .expiryDate(request.getExpiryDate())
                .isActive(true)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Coupon created: {} with {} units", savedCoupon.getCode(), savedCoupon.getTotalQuantity());

        return mapToResponse(savedCoupon);
    }

    /**
     * Get coupon details by code
     */
    @Transactional(readOnly = true)
    public CouponDTO.Response getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + code));
        return mapToResponse(coupon);
    }

    /**
     * Get all active and available coupons
     */
    @Transactional(readOnly = true)
    public List<CouponDTO.Response> getAvailableCoupons() {
        List<Coupon> coupons = couponRepository.findAvailableCoupons(LocalDateTime.now());
        return coupons.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * CRITICAL: Claim a coupon - handles race conditions with pessimistic locking
     * 
     * This method demonstrates how to prevent race conditions when multiple users
     * try to claim the same coupon simultaneously.
     * 
     * Strategy:
     * 1. Use PESSIMISTIC_WRITE lock to ensure only one thread can claim at a time
     * 2. Use SERIALIZABLE isolation level for strictest consistency
     * 3. Check all conditions (expiry, availability, duplicate claim) inside transaction
     * 4. Increment quantity and save in same transaction (atomic operation)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CouponDTO.ClaimResponse claimCoupon(Long userId, CouponDTO.ClaimRequest request) {
        log.info("User {} attempting to claim coupon: {}", userId, request.getCouponCode());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // LOCK THE COUPON: Pessimistic write lock prevents other threads from modifying it
        Coupon coupon = couponRepository.findByCodeWithLock(request.getCouponCode())
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + request.getCouponCode()));

        try {
            // Check 1: Coupon is active
            if (!coupon.getIsActive()) {
                log.warn("Coupon {} is not active", coupon.getCode());
                return buildClaimResponse(coupon, false, "Coupon is no longer active");
            }

            // Check 2: Coupon hasn't expired
            if (LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
                log.warn("Coupon {} has expired", coupon.getCode());
                return buildClaimResponse(coupon, false, "Coupon has expired");
            }

            // Check 3: Quantity still available (critical - must check inside transaction)
            if (coupon.getClaimedQuantity() >= coupon.getTotalQuantity()) {
                log.warn("Coupon {} is out of stock", coupon.getCode());
                return buildClaimResponse(coupon, false, "Coupon is out of stock");
            }

            // Check 4: User hasn't already claimed this coupon
            if (claimRepository.hasUserClaimedCoupon(user.getId(), coupon.getId())) {
                log.warn("User {} already claimed coupon {}", userId, coupon.getCode());
                return buildClaimResponse(coupon, false, "You have already claimed this coupon");
            }

            // ✅ All checks passed - claim the coupon
            // Create claim record
            CouponClaim claim = CouponClaim.builder()
                    .user(user)
                    .coupon(coupon)
                    .claimedAt(LocalDateTime.now())
                    .build();

            CouponClaim savedClaim = claimRepository.save(claim);

            // Increment coupon's claimed quantity
            coupon.incrementClaimedQuantity();
            couponRepository.save(coupon);

            log.info("Coupon {} successfully claimed by user {}. Claimed: {}/{}", 
                    coupon.getCode(), user.getEmail(), coupon.getClaimedQuantity(), coupon.getTotalQuantity());

            return CouponDTO.ClaimResponse.builder()
                    .claimId(savedClaim.getId())
                    .couponId(coupon.getId())
                    .couponCode(coupon.getCode())
                    .discountValue(coupon.getDiscountValue())
                    .discountType(coupon.getDiscountType())
                    .claimedAt(savedClaim.getClaimedAt())
                    .success(true)
                    .message("Coupon claimed successfully")
                    .build();

        } catch (OptimisticLockingFailureException e) {
            // This shouldn't happen with PESSIMISTIC_WRITE, but handled for safety
            log.error("Optimistic locking failure for coupon: {}", coupon.getCode(), e);
            throw new RuntimeException("Another user just claimed this coupon. Please try again.");
        } catch (Exception e) {
            log.error("Error claiming coupon: {}", request.getCouponCode(), e);
            throw new RuntimeException("Error claiming coupon: " + e.getMessage());
        }
    }

    /**
     * Get user's claimed coupons with coupon details (JOIN query)
     */
    @Transactional(readOnly = true)
    public List<CouponDTO.Response> getUserClaimedCoupons(Long userId) {
        List<CouponClaim> claims = claimRepository.findUserClaimsWithCoupons(userId);
        
        return claims.stream()
                .map(claim -> mapToResponse(claim.getCoupon()))
                .collect(Collectors.toList());
    }

    /**
     * Get paginated claim history for user
     */
    @Transactional(readOnly = true)
    public Page<CouponDTO.Response> getUserClaimHistory(Long userId, Pageable pageable) {
        return claimRepository.findByUserId(userId, pageable)
                .map(claim -> mapToResponse(claim.getCoupon()));
    }

    /**
     * Update coupon (admin operation)
     */
    @Transactional
    public CouponDTO.Response updateCoupon(Long couponId, CouponDTO.UpdateRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        if (request.getTotalQuantity() != null) {
            // Prevent reducing below claimed quantity
            if (request.getTotalQuantity() < coupon.getClaimedQuantity()) {
                throw new IllegalArgumentException(
                        "Cannot reduce total quantity below claimed quantity: " + coupon.getClaimedQuantity());
            }
            coupon.setTotalQuantity(request.getTotalQuantity());
        }

        if (request.getExpiryDate() != null) {
            coupon.setExpiryDate(request.getExpiryDate());
        }

        if (request.getIsActive() != null) {
            coupon.setIsActive(request.getIsActive());
        }

        Coupon updated = couponRepository.save(coupon);
        log.info("Coupon updated: {}", updated.getCode());

        return mapToResponse(updated);
    }

    /**
     * Get coupons expiring soon (7 days)
     */
    @Transactional(readOnly = true)
    public List<CouponDTO.Response> getExpiringCoupons() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);
        
        List<Coupon> coupons = couponRepository.findExpiringCoupons(now, sevenDaysLater);
        return coupons.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    private CouponDTO.Response mapToResponse(Coupon coupon) {
        return CouponDTO.Response.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountValue(coupon.getDiscountValue())
                .discountType(coupon.getDiscountType())
                .totalQuantity(coupon.getTotalQuantity())
                .claimedQuantity(coupon.getClaimedQuantity())
                .remainingQuantity(coupon.getRemainingQuantity())
                .expiryDate(coupon.getExpiryDate())
                .isActive(coupon.getIsActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    private CouponDTO.ClaimResponse buildClaimResponse(Coupon coupon, boolean success, String message) {
        return CouponDTO.ClaimResponse.builder()
                .couponId(coupon.getId())
                .couponCode(coupon.getCode())
                .discountValue(coupon.getDiscountValue())
                .discountType(coupon.getDiscountType())
                .success(success)
                .message(message)
                .build();
    }
}
