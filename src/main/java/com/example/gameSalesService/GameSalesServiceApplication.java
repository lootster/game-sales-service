package com.example.gameSalesService;

import com.example.gameSalesService.service.CacheWarmupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching  // Enabling Spring Cache support

public class GameSalesServiceApplication implements CommandLineRunner {

	@Autowired
	private CacheWarmupService cacheWarmupService;

	public static void main(String[] args) {
		SpringApplication.run(GameSalesServiceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Pre-warm cache at application startup
		cacheWarmupService.preWarmCache();
	}
}
