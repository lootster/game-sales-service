package com.example.gameSalesService.controller;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import com.example.gameSalesService.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")  // Optional base path to organize the endpoints
@EnableAsync
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

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
        Instant start = Instant.now();  // Record start time

        try {
            // Save the file temporarily for processing
            Path tempFile = Files.createTempFile("game_sales_import_", ".csv");
            Files.write(tempFile, file.getBytes());

            // Call the ImportService to handle the import asynchronously
            importService.processFileAsync(tempFile.toString(), start);  // Passing start time

            return ResponseEntity.status(HttpStatus.ACCEPTED).body("File received, processing in the background...");
        } catch (Exception e) {
            logger.error("Failed to process import CSV request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to import file");
        }
    }

    @Cacheable(value = "gameSalesCache", key = "#page + '-' + #size + '-' + #fromDate + '-' + #toDate + '-' + #salePrice + '-' + #filter")
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
