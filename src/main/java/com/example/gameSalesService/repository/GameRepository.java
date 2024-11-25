package com.example.gameSalesService.repository;

import com.example.gameSalesService.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    // You can add custom query methods here if necessary
}
