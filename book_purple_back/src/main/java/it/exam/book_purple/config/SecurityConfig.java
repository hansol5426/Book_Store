package it.exam.book_purple.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import it.exam.book_purple.common.utils.JWTUtils;
import it.exam.book_purple.filter.CustomLogoutFilter;
import it.exam.book_purple.filter.JWTFilter;
import it.exam.book_purple.filter.LoginFilter;
import it.exam.book_purple.security.service.UserServiceDetails;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserServiceDetails serviceDetails;
    private final JWTUtils jwtUtils;

    // 시큐리티 우선 무시하기
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return web -> 
                web.ignoring()
                .requestMatchers("/static/img/**")
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());  
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        AuthenticationConfiguration configuration
            = http.getSharedObject(AuthenticationConfiguration.class);
        
        // loginFilter에서 인증처리 하기 위한 매니저 생성
        AuthenticationManager manager = this.authenticationManager(configuration);

        // 로그인 필터 생성 및 경로 저장
        LoginFilter loginfilter = new LoginFilter(manager, jwtUtils);
        loginfilter.setFilterProcessesUrl("/api/v1/login");

        // 기본 보안 기능 끄기
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(this.configurationSource()))
            .httpBasic(AbstractHttpConfigurer::disable) 
            .formLogin(AbstractHttpConfigurer::disable) 
            .authorizeHttpRequests(
                auth ->
                    auth.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/api/v1/login/**").permitAll()
                        .requestMatchers("/api/v1/logout/**").permitAll()
                        .requestMatchers("/api/v1/refresh").permitAll()
                        .anyRequest().authenticated()

            // 필터 순서 설정           
            ).addFilterBefore(new JWTFilter(jwtUtils), LoginFilter.class)
            // UsernamePasswordAuthenticationFilter 대신 Loginfilter를 실행해라
            // Loginfilter에 UsernamePasswordAuthenticationFilter 상속받음
            .addFilterAt(loginfilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new CustomLogoutFilter(jwtUtils), LogoutFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .logout(withDefaults());

            return http.build();
    }   
    
    // auth provider 생성해서 전달 > 사용자가 만든것을 전달
    @Bean
    public AuthenticationProvider authProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(serviceDetails);
        provider.setPasswordEncoder(bcyPasswordEncoder());
        return provider;
    }

    // 패스워드 암호화 객체 설정
    @Bean
    public PasswordEncoder bcyPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    // 로그인필터가 매니저를 가지고 일을 해서 필요함
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource configurationSource(){
        CorsConfiguration config = new CorsConfiguration();

        // 헤더 설정
        config.setAllowedHeaders(List.of("*"));
        // 메서드 설정
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));
        // 인터넷 경로
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001","http://localhost:4000", "http://localhost:4001"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return source;
    }


}
