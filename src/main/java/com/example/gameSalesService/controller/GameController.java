package com.example.gameSalesService.controller;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
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
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @GetMapping("/health")
    public String healthCheck() {
        return "Application is running!";
    }

    @PostMapping("/import")
    public ResponseEntity<String> importCsv(@RequestParam("file") MultipartFile file) {
        try {
            List<Game> games = new ArrayList<>();

            // Read the CSV file line by line
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Skip the header line
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Split the CSV line
                    String[] fields = line.split(",");

                    // Ensure the record has the expected number of fields
                    if (fields.length != 9) {
                        continue;
                    }

                    // Create a new Game object from the CSV fields
                    Game game = new Game();
                    game.setId(Long.parseLong(fields[0]));
                    game.setGameNo(Integer.parseInt(fields[1]));
                    game.setGameName(fields[2]);
                    game.setGameCode(fields[3]);
                    game.setType(Integer.parseInt(fields[4]));
                    game.setCostPrice(Double.parseDouble(fields[5]));
                    game.setTax(Double.parseDouble(fields[6]));
                    game.setSalePrice(Double.parseDouble(fields[7]));
                    game.setDateOfSale(LocalDateTime.parse(fields[8]));

                    games.add(game);
                }
            }

            // Save all games to the database
            gameRepository.saveAll(games);

            return ResponseEntity.ok("File imported and records saved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to import file and save records");
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
