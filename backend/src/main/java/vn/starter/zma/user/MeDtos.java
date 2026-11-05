package vn.starter.zma.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MeDtos {
    private MeDtos() {}

    public record UpdateMeRequest(@NotBlank(message = "Tên không được để trống") @Size(min = 2, max = 150, message = "Tên từ 2–150 ký tự") String name) {}

    /** token: từ zmp-sdk getPhoneNumber/getLocation; zaloAccessToken: từ getAccessToken (backend không lưu token Zalo). */
    public record DecodeTokenRequest(@NotBlank(message = "Thiếu token") @Size(max = 4096) String token,
                                     @NotBlank(message = "Thiếu access token Zalo") @Size(max = 4096) String zaloAccessToken) {}
}
