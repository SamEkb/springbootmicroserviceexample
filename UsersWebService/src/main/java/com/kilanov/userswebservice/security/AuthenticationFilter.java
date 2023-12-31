package com.kilanov.userswebservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilanov.userswebservice.service.UserService;
import com.kilanov.userswebservice.ui.requests.LoginRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final UserService userService;
    private final Environment environment;

    public AuthenticationFilter(AuthenticationManager manager, UserService userService, Environment environment) {
        super(manager);
        this.userService = userService;
        this.environment = environment;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response
    ) throws AuthenticationException {
        try {
            LoginRequest credentials = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            credentials.getEmail(),
                            credentials.getPassword()
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult
    ) {
        String userName = ((User) authResult.getPrincipal()).getUsername();
        String tokenSecret = environment.getProperty("token.secret");
        var userDetails = userService.getUserByEmail(userName);
        var tokenSecretBytes = Base64.getEncoder().encode(tokenSecret.getBytes());
        SecretKey secretKey = new SecretKeySpec(tokenSecretBytes, SignatureAlgorithm.HS512.getJcaName());

        String token = Jwts.builder()
                .claim("scope", authResult.getAuthorities())
                .setSubject(userDetails.getUserId())
                .setExpiration(Date.from(
                        Instant.now().plusMillis(Long.parseLong(environment.getProperty("token.expiration_time"))))
                )
                .setIssuedAt(Date.from(Instant.now()))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        response.addHeader("token", token);
        response.addHeader("publicUserId", userDetails.getUserId());
    }
}
