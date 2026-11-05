package vn.bookeasy.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import vn.bookeasy.support.IntegrationTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registerLoginAndMe() throws Exception {
        Shop s = createShop();
        JsonNode me = ok(get("/api/auth/me"), s.ownerToken(), null);
        assertThat(me.get("role").asText()).isEqualTo("OWNER");
        assertThat(me.get("businessSlug").asText()).isEqualTo(s.slug());
        String token = login(s.slug() + "@test.vn", "Matkhau123");
        assertThat(token).isNotBlank();
    }

    @Test
    void rejectsBadCredentialsAndValidation() throws Exception {
        call(post("/api/auth/login"), null, Map.of("email", "nobody@test.vn", "password", "Matkhau123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
        call(post("/api/auth/register"), null, Map.of("businessName", "X", "slug", "Bad Slug!", "fullName", "A",
                "email", "not-an-email", "password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.slug").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void duplicateSlugIsConflict() throws Exception {
        Shop s = createShop();
        call(post("/api/auth/register"), null, Map.of("businessName", "Khác", "slug", s.slug(), "fullName", "B",
                "email", "other-" + s.slug() + "@test.vn", "password", "Matkhau123"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLUG_TAKEN"));
    }

    @Test
    void refreshRotatesAndDetectsReuse() throws Exception {
        Shop s = createShop();
        JsonNode first = ok(post("/api/auth/login"), null, Map.of("email", s.slug() + "@test.vn", "password", "Matkhau123"));
        String rt1 = first.get("refreshToken").asText();
        JsonNode second = ok(post("/api/auth/refresh"), null, Map.of("refreshToken", rt1));
        String rt2 = second.get("refreshToken").asText();
        assertThat(rt2).isNotEqualTo(rt1);
        // dùng lại token cũ → bị từ chối và toàn bộ phiên bị thu hồi
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", rt1)).andExpect(status().isUnauthorized());
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", rt2)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Shop s = createShop();
        String rt = ok(post("/api/auth/login"), null, Map.of("email", s.slug() + "@test.vn", "password", "Matkhau123"))
                .get("refreshToken").asText();
        call(post("/api/auth/logout"), null, Map.of("refreshToken", rt)).andExpect(status().isNoContent());
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", rt)).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAuthAndRole() throws Exception {
        call(get("/api/bookings?from=2030-01-01T00:00:00&to=2030-01-02T00:00:00"), null, null).andExpect(status().isUnauthorized());
        call(get("/api/business"), "garbage.token.value", null).andExpect(status().isUnauthorized());
        Shop s = createShop();
        call(get("/api/admin/businesses"), s.ownerToken(), null).andExpect(status().isForbidden());
    }
}
