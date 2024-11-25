package com.example.gameSalesService.repository;

import com.example.gameSalesService.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    // You can add custom query methods here if necessary
    Page<Game> findAllByDateOfSaleBetween(LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Page<Game> findAllBySalePriceGreaterThan(Double salePrice, Pageable pageable);

    Page<Game> findAllBySalePriceLessThan(Double salePrice, Pageable pageable);
}


