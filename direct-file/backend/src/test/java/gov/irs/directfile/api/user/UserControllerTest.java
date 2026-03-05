package gov.irs.directfile.api.user;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.api.user.domain.UserInfo;
import gov.irs.directfile.api.user.domain.UserInfoResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void givenAuthenticatedUser_whenGetUserInfo_thenReturnsUserInfo() {
        // given
        UUID id = UUID.randomUUID();
        UUID externalId = UUID.randomUUID();
        String email = "test@example.com";
        String tin = "123456789";
        UserInfo userInfo = new UserInfo(id, externalId, email, tin);
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);

        // when
        UserInfoResponse response = userController.getUserInfo();

        // then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(email);
    }

    @Test
    void givenServiceThrows_whenGetUserInfo_thenExceptionPropagates() {
        // given
        when(userService.getCurrentUserInfo()).thenThrow(new RuntimeException("auth failure"));

        // when / then
        assertThatThrownBy(() -> userController.getUserInfo())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("auth failure");
    }
}
