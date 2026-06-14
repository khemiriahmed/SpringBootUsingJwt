package com.akh.SpringBootUsingJwt.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
@GetMapping("/")
public String home() {
	return "Home page";
}
@GetMapping("/store")
public String store() {
	return "store page";
}
@GetMapping("/admin/home")
public String getAdminHome() { 
	return "Admin Home Page";
 }
@GetMapping("/client/home")
public String getClientHome() {
	return "ClientHome Page";
}
}


