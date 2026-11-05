package vn.starter.zma.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.util.PageRequests;
import vn.starter.zma.common.util.PhoneUtils;
import vn.starter.zma.zalo.ZaloGraphClient;
import vn.starter.zma.zalo.ZaloLocation;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ZaloGraphClient zaloGraphClient;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public UserDto updateName(Long id, String name) {
        User u = find(id);
        u.setName(name.trim());
        return mapper.toDto(u);
    }

    @Transactional
    public UserDto linkPhone(Long id, String zaloAccessToken, String phoneToken) {
        User u = find(id);
        assertSameZaloUser(u, zaloAccessToken);
        String phone = PhoneUtils.normalizeVn(zaloGraphClient.decodePhoneNumber(zaloAccessToken, phoneToken));
        u.setPhone(phone);
        log.info("User {} linked phone {}", id, PhoneUtils.mask(phone));
        return mapper.toDto(u);
    }

    @Transactional
    public UserDto updateLocation(Long id, String zaloAccessToken, String locationToken) {
        User u = find(id);
        assertSameZaloUser(u, zaloAccessToken);
        ZaloLocation loc = zaloGraphClient.decodeLocation(zaloAccessToken, locationToken);
        u.setLastLat(loc.latitude());
        u.setLastLng(loc.longitude());
        return mapper.toDto(u);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> search(String q, Role role, int page, int size) {
        return PageResponse.of(userRepository.search(PageRequests.likePattern(q), role,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"))), mapper::toDto);
    }

    /** Chống dùng access token Zalo của người khác để ghi SĐT vào tài khoản mình. */
    private void assertSameZaloUser(User u, String zaloAccessToken) {
        String zaloId = zaloGraphClient.getProfile(zaloAccessToken).id();
        if (!zaloId.equals(u.getZaloId())) {
            throw new AppException(vn.starter.zma.common.exception.ErrorCode.FORBIDDEN, "Token Zalo không thuộc tài khoản này");
        }
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> AppException.notFound("người dùng"));
    }
}
