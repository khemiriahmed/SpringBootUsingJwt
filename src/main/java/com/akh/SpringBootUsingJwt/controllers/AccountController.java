package com.akh.SpringBootUsingJwt.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
public class AccountController {
 @Value("${security.jwt.secret-key}")
 private String jwtSecretKey;
 
 @Value("${security.jwt.issuer}")
 private String jwtIssuer;
}
