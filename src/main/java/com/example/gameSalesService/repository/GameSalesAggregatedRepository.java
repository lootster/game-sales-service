package com.example.gameSalesService.repository;

import com.example.gameSalesService.entity.GameSalesAggregated;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GameSalesAggregatedRepository extends CrudRepository<GameSalesAggregated, Long> {

    // Fetches the sales count for a given period.
    List<GameSalesAggregated> findSalesCountByDateOfSaleBetween(LocalDate fromDate, LocalDate toDate);

    // Fetches the aggregated total sales for a given period.
    List<GameSalesAggregated> findTotalSalesByDateOfSaleBetween(LocalDate fromDate, LocalDate toDate);

    // Fetches the aggregated total sales for a specific game_no for a given period.
    List<GameSalesAggregated> findTotalSalesByDateOfSaleBetweenAndGameNo(LocalDate fromDate, LocalDate toDate, Integer gameNo);
}
