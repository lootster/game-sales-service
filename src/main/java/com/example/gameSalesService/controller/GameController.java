package com.example.gameSalesService.controller;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import com.example.gameSalesService.entity.GameSalesAggregated;
import com.example.gameSalesService.repository.GameSalesAggregatedRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private GameSalesAggregatedRepository gameSalesAggregatedRepository;

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

        long startTime = System.currentTimeMillis();  // Start time

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

        logExecutionTime("Time taken for /getGameSales: {} ms", startTime);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/getTotalSales")
    public ResponseEntity<Object> getTotalSales(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(required = false) Integer gameNo,
            @RequestParam(defaultValue = "totalSales") String filter) {

        long startTime = System.currentTimeMillis();  // Start time

        Map<String, Object> result = new HashMap<>();

        // Check if filter is "salesCount" and gameNo is provided together
        if ("salesCount".equalsIgnoreCase(filter) && gameNo != null) {
            logExecutionTime("Time taken for /getTotalSales: {} ms", startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "To get sales count, gameNo must not be included."));
        }

        // Check if the filter is "salesCount"
        if ("salesCount".equalsIgnoreCase(filter)) {
            // Get the total games sold for the given period
            int totalGamesSold = gameSalesAggregatedRepository
                    .findSalesCountByDateOfSaleBetween(fromDate, toDate)
                    .stream()
                    .mapToInt(GameSalesAggregated::getTotalGamesSold)
                    .sum();

            // If no data found
            if (totalGamesSold == 0) {
                logExecutionTime("Time taken for /getTotalSales: {} ms", startTime);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No sales count data found for the given period.");
            }
            result.put("totalGamesSold", totalGamesSold);
        } else {
            // Get the total sales for the given period
            double totalSales;

            if (gameNo != null) {
                // Fetch total sales for a specific game number
                totalSales = gameSalesAggregatedRepository
                        .findTotalSalesByDateOfSaleBetweenAndGameNo(fromDate, toDate, gameNo)
                        .stream()
                        .mapToDouble(GameSalesAggregated::getTotalSales)
                        .sum();
            } else {
                // Fetch total sales for all games in the given period
                totalSales = gameSalesAggregatedRepository
                        .findTotalSalesByDateOfSaleBetween(fromDate, toDate)
                        .stream()
                        .mapToDouble(GameSalesAggregated::getTotalSales)
                        .sum();
            }

            // If no data found
            if (totalSales == 0) {
                logExecutionTime("Time taken for /getTotalSales: {} ms", startTime);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No total sales data found for the given period.");
            }

            BigDecimal roundedTotalSales = BigDecimal.valueOf(totalSales).setScale(2, RoundingMode.HALF_UP);
            result.put("totalSales", roundedTotalSales.toPlainString());
        }

        logExecutionTime("Time taken for /getTotalSales: {} ms", startTime);

        return ResponseEntity.ok(result);
    }

    private static void logExecutionTime(String s, long startTime) {
        long endTime = System.currentTimeMillis();  // End time
        logger.info(s, (endTime - startTime));
    }
}
