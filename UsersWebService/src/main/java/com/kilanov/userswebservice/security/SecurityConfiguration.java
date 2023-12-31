package com.kilanov.userswebservice.security;

import com.kilanov.userswebservice.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
public class SecurityConfiguration {
    private final UserService userService;
    private final Environment environment;
    private final BCryptPasswordEncoder encoder;

    public SecurityConfiguration(UserService userService, Environment environment, BCryptPasswordEncoder encoder) {
        this.userService = userService;
        this.environment = environment;
        this.encoder = encoder;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService)
                .passwordEncoder(encoder);

        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(authenticationManager, userService,
                environment);
        authenticationFilter.setFilterProcessesUrl(environment.getProperty("login.url_path"));

        AuthorizationFilter authorizationFilter = new AuthorizationFilter(authenticationManager, environment);

        return http.csrf(AbstractHttpConfigurer::disable)
                .headers(header -> header.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(it -> it.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                            auth.requestMatchers(antMatcher("/users")).permitAll();
                            auth.requestMatchers(antMatcher("/actuator/**")).permitAll();
                            auth.requestMatchers(antMatcher("/users/**")).permitAll();
                            auth.requestMatchers(antMatcher("/h2-console/**")).permitAll();
                        }
                )
                .addFilter(authenticationFilter)
                .addFilter(authorizationFilter)
                .authenticationManager(authenticationManager)
                .build();
    }
}
