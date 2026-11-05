package vn.starter.zma.user;

import java.math.BigDecimal;
import java.time.Instant;

public record UserDto(Long id, String zaloId, String email, String name, String avatarUrl, String phone,
                      Role role, UserStatus status, BigDecimal lastLat, BigDecimal lastLng, Instant createdAt) {}
