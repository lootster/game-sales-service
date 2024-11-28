package com.example.gameSalesService.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "game_sales")
public class Game {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "game_no", nullable = false)
    private Integer gameNo;

    @Column(name = "game_name", length = 20, nullable = false)
    private String gameName;

    @Column(name = "game_code", length = 5, nullable = false)
    private String gameCode;

    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "cost_price", nullable = false)
    private Double costPrice;

    @Column(name = "tax", nullable = false)
    private Double tax;

    @Column(name = "sale_price", nullable = false)
    private Double salePrice;

    @Column(name = "date_of_sale", nullable = false)
    private LocalDate dateOfSale;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getGameNo() {
        return gameNo;
    }

    public void setGameNo(Integer gameNo) {
        this.gameNo = gameNo;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Double costPrice) {
        this.costPrice = costPrice;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Double salePrice) {
        this.salePrice = salePrice;
    }

    public LocalDate getDateOfSale() {
        return dateOfSale;
    }

    public void setDateOfSale(LocalDate dateOfSale) {
        this.dateOfSale = dateOfSale;
    }
}