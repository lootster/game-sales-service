package com.example.gameSalesService.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "game_sales_aggregated", indexes = {
        @Index(name = "idx_date_of_sale", columnList = "date_of_sale"),
        @Index(name = "idx_game_no", columnList = "game_no")
})
public class GameSalesAggregated {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date_of_sale", nullable = false)
    private LocalDate dateOfSale;

    @Column(name = "total_games_sold", nullable = false)
    private Integer totalGamesSold;

    @Column(name = "total_sales", nullable = false)
    private Double totalSales;

    @Column(name = "game_no", nullable = true)
    private Integer gameNo;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateOfSale() {
        return dateOfSale;
    }

    public void setDateOfSale(LocalDate dateOfSale) {
        this.dateOfSale = dateOfSale;
    }

    public Integer getTotalGamesSold() {
        return totalGamesSold;
    }

    public void setTotalGamesSold(Integer totalGamesSold) {
        this.totalGamesSold = totalGamesSold;
    }

    public Double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getGameNo() {
        return gameNo;
    }

    public void setGameNo(Integer gameNo) {
        this.gameNo = gameNo;
    }
}
