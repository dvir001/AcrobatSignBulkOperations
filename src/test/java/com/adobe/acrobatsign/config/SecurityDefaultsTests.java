package com.adobe.acrobatsign.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.net.InetAddress;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(OutputCaptureExtension.class)
class SecurityDefaultsTests {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void missingPackagedConfigurationStillBindsToLoopback() throws Exception {
        ConfigurableWebServerFactory factory = mock(ConfigurableWebServerFactory.class);
        config.loopbackByDefault(new MockEnvironment()).customize(factory);
        verify(factory).setAddress(InetAddress.getByName("127.0.0.1"));
    }

    @Test
    void explicitBindingIsPreserved() {
        ConfigurableWebServerFactory factory = mock(ConfigurableWebServerFactory.class);
        config.loopbackByDefault(new MockEnvironment().withProperty("server.address", "192.0.2.1"))
                .customize(factory);
        verifyNoInteractions(factory);
    }

    @Test
    void blankPasswordGeneratesUsableRandomCredential(CapturedOutput output) {
        var encoder = config.passwordEncoder();
        var first = config.localOperator("operator", "", encoder).loadUserByUsername("operator");
        var second = config.localOperator("operator", "", encoder).loadUserByUsername("operator");
        var generated = Pattern.compile("Generated local operator password: ([a-f0-9-]{36})").matcher(output.getAll());
        assertThat(generated.find()).isTrue();
        String firstPassword = generated.group(1);
        assertThat(encoder.matches(firstPassword, first.getPassword())).isTrue();
        assertThat(generated.find()).isTrue();
        assertThat(generated.group(1)).isNotEqualTo(firstPassword);
        assertThat(encoder.matches(generated.group(1), second.getPassword())).isTrue();
        assertThat(first.getAuthorities()).extracting("authority").containsExactly("ROLE_OPERATOR");
    }

    @Test
    void configuredCredentialIsHashedAndNeverLogged(CapturedOutput output) {
        var encoder = config.passwordEncoder();
        String password = "configured-test-only-password";
        var user = config.localOperator("configured-operator", password, encoder)
                .loadUserByUsername("configured-operator");
        assertThat(user.getPassword()).isNotEqualTo(password);
        assertThat(encoder.matches(password, user.getPassword())).isTrue();
        assertThat(output.getAll()).doesNotContain(password);
    }
}
