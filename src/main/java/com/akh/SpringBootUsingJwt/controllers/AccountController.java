package com.akh.SpringBootUsingJwt.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akh.SpringBootUsingJwt.models.AppUser;
import com.akh.SpringBootUsingJwt.models.LoginDto;
import com.akh.SpringBootUsingJwt.models.RegisterDto;
import com.akh.SpringBootUsingJwt.repositories.AppUserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Value("${security.jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    @Autowired
    private AppUserRepository repo;

    @Autowired
    private AuthenticationManager authenticationManager;

    // GET /account/profile  (nécessite un JWT valide dans le header Authorization)
    @GetMapping("/profile")
    public ResponseEntity<Object> profile(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        AppUser appUser = repo.findByUsername(username);

        if (appUser == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(appUser);
    }

    // POST /account/register
    @PostMapping("/register")
    public ResponseEntity<Object> register(
            @Valid @RequestBody RegisterDto registerDto,
            BindingResult result) {

        if (result.hasErrors()) {
            var errorsMap = new HashMap<String, String>();
            var errorList = result.getAllErrors();
            for (int i = 0; i < errorList.size(); i++) {
                var error = (FieldError) errorList.get(i);
                errorsMap.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errorsMap);
        }

        if (repo.findByEmail(registerDto.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Map.of("email", "Email already exists"));
        }

        try {
            var bCryptEncoder = new BCryptPasswordEncoder();
            AppUser appUser = new AppUser();
            appUser.setFirstName(registerDto.getFirstName());
            appUser.setLastName(registerDto.getLastName());
            appUser.setUsername(registerDto.getUsername());
            appUser.setEmail(registerDto.getEmail());
            appUser.setPhone(registerDto.getPhone());
            appUser.setAddress(registerDto.getAddress());
            appUser.setRole("client");
            appUser.setCreatedAt(new Date());
            appUser.setPassword(bCryptEncoder.encode(registerDto.getPassword()));

            repo.save(appUser);

            String token = createJwtToken(appUser);
            return ResponseEntity.ok(Map.of("token", token, "tokenType", "Bearer", "user", appUser));

        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    // POST /account/login
    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @Valid @RequestBody LoginDto loginDto,
            BindingResult result) {

        if (result.hasErrors()) {
            var errorsMap = new HashMap<String, String>();
            var errorList = result.getAllErrors();
            for (int i = 0; i < errorList.size(); i++) {
                var error = (FieldError) errorList.get(i);
                errorsMap.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errorsMap);
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginDto.getUsername(),
                    loginDto.getPassword()
                )
            );

            AppUser appUser = repo.findByUsername(loginDto.getUsername());
            String token = createJwtToken(appUser);
            return ResponseEntity.ok(Map.of("token", token, "tokenType", "Bearer", "user", appUser));

        } catch (AuthenticationException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad username or password"));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    private String createJwtToken(AppUser appUser) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.DAYS))
                .subject(appUser.getUsername())
                .claim("role", appUser.getRole())
                .build();

        var secretKey = new SecretKeySpec(jwtSecretKey.getBytes(), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));

        var params = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        );

        return encoder.encode(params).getTokenValue();
    }
}