package vn.starter.zma.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.starter.zma.security.CurrentUser;
import vn.starter.zma.user.MeDtos.DecodeTokenRequest;
import vn.starter.zma.user.MeDtos.UpdateMeRequest;

@Tag(name = "Me")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {
    private final UserService userService;

    @GetMapping
    public UserDto me() {
        return userService.get(CurrentUser.get().id());
    }

    @PatchMapping
    public UserDto update(@Valid @RequestBody UpdateMeRequest req) {
        return userService.updateName(CurrentUser.get().id(), req.name());
    }

    @Operation(summary = "Giải mã token số điện thoại (zmp-sdk getPhoneNumber) và lưu vào hồ sơ")
    @PostMapping("/phone")
    public UserDto phone(@Valid @RequestBody DecodeTokenRequest req) {
        return userService.linkPhone(CurrentUser.get().id(), req.zaloAccessToken(), req.token());
    }

    @Operation(summary = "Giải mã token vị trí (zmp-sdk getLocation)")
    @PostMapping("/location")
    public UserDto location(@Valid @RequestBody DecodeTokenRequest req) {
        return userService.updateLocation(CurrentUser.get().id(), req.zaloAccessToken(), req.token());
    }
}
