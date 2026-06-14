package com.akh.SpringBootUsingJwt.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akh.SpringBootUsingJwt.models.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

	public AppUser findByEmail(String email);
	public AppUser findByUsername(String username);
}