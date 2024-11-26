package com.example.gameSalesService.controller;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import com.example.gameSalesService.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")  // Optional base path to organize the endpoints
@EnableAsync
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ImportService importService;  // A new service to handle importing in the background

    @GetMapping("/health")
    public String healthCheck() {
        return "Application is running!";
    }

    @PostMapping("/import")
    public ResponseEntity<String> importCsv(@RequestParam("file") MultipartFile file) {
        try {
            // Save file to temporary storage for asynchronous processing
            String tempFilePath = "/tmp/" + file.getOriginalFilename();
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);

            // Process asynchronously
            importService.processFileAsync(tempFilePath);

            return ResponseEntity.accepted().body("File received, processing in the background...");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to receive file");
        }
    }



    @GetMapping("/getGameSales")
    public ResponseEntity<Map<String, Object>> getGameSales(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Double salePrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String filter) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Game> gamePage;

        if (salePrice != null && filter != null) {
            // Handle filtering by sale price
            if (filter.equalsIgnoreCase("greaterThan")) {
                gamePage = gameRepository.findAllBySalePriceGreaterThan(salePrice, pageable);
            } else if (filter.equalsIgnoreCase("lessThan")) {
                gamePage = gameRepository.findAllBySalePriceLessThan(salePrice, pageable);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid filter value"));
            }
        } else if (fromDate != null && toDate != null) {
            // Handle filtering by date range
            gamePage = gameRepository.findAllByDateOfSaleBetween(fromDate, toDate, pageable);
        } else {
            // Default case: return all games with pagination
            gamePage = gameRepository.findAll(pageable);
        }

        // Prepare response with additional metadata
        Map<String, Object> response = new HashMap<>();
        response.put("games", gamePage.getContent());
        response.put("currentPage", gamePage.getNumber());
        response.put("totalItems", gamePage.getTotalElements());
        response.put("totalPages", gamePage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }




}
