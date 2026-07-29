package com.luarc.controller;

import com.luarc.dto.CouponDTO;
import com.luarc.security.UserPrincipal;
import com.luarc.service.CouponService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
@Slf4j
public class CouponController {

    @Autowired
    private CouponService couponService;

    /**
     * Create a new coupon (admin only)
     * POST /api/coupons/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCoupon(@Valid @RequestBody CouponDTO.CreateRequest request) {
        try {
            log.info("Creating coupon with code: {}", request.getCode());
            CouponDTO.Response response = couponService.createCoupon(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Coupon creation failed: {}", e.getMessage());
            return new ResponseEntity<>(
                    new ErrorResponse(e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Error creating coupon", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Error creating coupon: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get coupon by code
     * GET /api/coupons/code/{code}
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<?> getCouponByCode(@PathVariable String code) {
        try {
            CouponDTO.Response response = couponService.getCouponByCode(code);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching coupon: {}", code, e);
            return new ResponseEntity<>(
                    new ErrorResponse(e.getMessage()),
                    HttpStatus.NOT_FOUND
            );
        }
    }

    /**
     * Get all available coupons
     * GET /api/coupons/available
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableCoupons() {
        try {
            List<CouponDTO.Response> coupons = couponService.getAvailableCoupons();
            return new ResponseEntity<>(coupons, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching available coupons", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Error fetching coupons: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * CRITICAL ENDPOINT: Claim a coupon
     * POST /api/coupons/claim
     * This endpoint handles concurrent claims with race condition prevention
     */
    @PostMapping("/claim")
    public ResponseEntity<?> claimCoupon(
            @Valid @RequestBody CouponDTO.ClaimRequest request,
            Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getId();

            log.info("User {} attempting to claim coupon: {}", userId, request.getCouponCode());

            CouponDTO.ClaimResponse response = couponService.claimCoupon(userId, request);
            
            if (response.getSuccess()) {
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } catch (RuntimeException e) {
            log.error("Error claiming coupon: {}", request.getCouponCode(), e);
            return new ResponseEntity<>(
                    new ErrorResponse(e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Unexpected error claiming coupon", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Error claiming coupon: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get user's claimed coupons
     * GET /api/coupons/my-coupons
     */
    @GetMapping("/my-coupons")
    public ResponseEntity<?> getMyClaimedCoupons(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getId();

            List<CouponDTO.Response> coupons = couponService.getUserClaimedCoupons(userId);
            return new ResponseEntity<>(coupons, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching user coupons", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Error fetching your coupons: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get paginated claim history
     * GET /api/coupons/my-claims?page=0&size=10
     */
    @GetMapping("/my-claims")
    public ResponseEntity<?> getClaimHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getId();

            Pageable pageable = PageRequest.of(page, size);
            Page<CouponDTO.Response> claims = couponService.getUserClaimHistory(userId, pageable);
            return new ResponseEntity<>(claims, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching claim history", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Error fetching claim history: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Update coupon
     * PUT /api/coupons/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponDTO.UpdateRequest request) {
        try {
            log.info("Updating coupon with id: {}", id);
            CouponDTO.Response response = couponService.updateCoupon(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error updating coupon", e);
            return new ResponseEntity<>(
                    new ErrorResponse(e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Get expiring coupons
     * GET /api/coupons/expiring-soon
     */
    @GetMapping("/expiring-soon")
    public ResponseEntity<?> getExpiringCoupons() {
        try {
            List<CouponDTO.Response> coupons = couponService.getExpiringCoupons();
            return new ResponseEntity<>(coupons, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching expiring coupons", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Error fetching expiring coupons: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Health check endpoint
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return new ResponseEntity<>(
                new HealthResponse("Coupon API is running"),
                HttpStatus.OK
        );
    }

    // ========== ERROR & RESPONSE CLASSES ==========

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }

    public static class HealthResponse {
        public String status;

        public HealthResponse(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}
