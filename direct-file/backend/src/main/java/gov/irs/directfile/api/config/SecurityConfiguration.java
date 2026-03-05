package gov.irs.directfile.api.config;

import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import gov.irs.directfile.api.auth.ApiClientRepository;
import gov.irs.directfile.api.auth.ApiKeyAuthenticationFilter;
import gov.irs.directfile.api.authentication.*;
import gov.irs.directfile.api.cache.CacheService;

@Configuration
@Profile(BeanProfiles.DEFAULT_SECURITY)
@EnableConfigurationProperties({UserDetailsCacheProperties.class})
@Slf4j
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class SecurityConfiguration {

    @Bean
    @Profile(BeanProfiles.FAKE_PII_SERVICE)
    public PIIService fakePiiService() {
        return new FakePIIService();
    }

    @Bean
    @Profile("!" + BeanProfiles.ENABLE_REMOTE_CACHE)
    public UserDetailsCacheService localUserDetailsCacheService(UserDetailsCacheProperties userDetailsCacheProperties) {
        return new LocalUserDetailsCacheService(userDetailsCacheProperties);
    }

    @Bean
    @Profile(BeanProfiles.ENABLE_REMOTE_CACHE)
    public UserDetailsCacheService remoteUserDetailsCacheService(
            CacheService cacheService, UserDetailsCacheProperties userDetailsCacheProperties) {
        return new RemoteUserDetailsCacheService(cacheService, userDetailsCacheProperties);
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${direct-file.auth.jwt.signing-key:default-dev-signing-key-must-be-at-least-32-bytes-long!!}")
                    String signingKey) {
        SecretKeySpec secretKey =
                new SecretKeySpec(signingKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        log.info("Adding SecurityFilterChain: publicFilterChain");
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .securityMatchers(matchers -> matchers.requestMatchers("/actuator/**", "/oauth/token"))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, ApiClientRepository apiClientRepository)
            throws Exception {
        log.info("Adding SecurityFilterChain: apiFilterChain");
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .securityMatchers(matchers -> matchers.requestMatchers("/v1/**", "/v2/**"))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterBefore(
                        new ApiKeyAuthenticationFilter(apiClientRepository), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        log.info("Adding SecurityFilterChain: defaultFilterChain");
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .securityMatchers(matchers -> matchers.requestMatchers("/**"))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
