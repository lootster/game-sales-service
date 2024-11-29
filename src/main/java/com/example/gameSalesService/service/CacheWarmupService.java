package com.example.gameSalesService.service;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CacheWarmupService {

    @Autowired
    private GameRepository gameRepository;

    public void preWarmCache() {
        // Pre-fetching first page
        cachePage(0);

        // Pre-fetching page 999
        cachePage(999);
    }

    @Cacheable("gameSalesCache")
    public Page<Game> cachePage(int pageNumber) {
        return gameRepository.findAll(PageRequest.of(pageNumber, 100));
    }
}
