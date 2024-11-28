package com.example.gameSalesService.service;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;

@Service
public class ImportService {

    private final Logger logger = LoggerFactory.getLogger(ImportService.class);

    @Autowired
    private GameRepository gameRepository;

    @Async
    public void processFileAsync(String filePath, Instant start) {
        try {
            int batchSize = 5000;  // Increased batch size for optimal performance
            ExecutorService executor = Executors.newFixedThreadPool(10);  // Increased threads to 20 for better performance

            List<Game> gamesBatch = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false; // Skip header
                        continue;
                    }

                    // Split and parse the CSV line
                    String[] fields = line.split(",");
                    if (fields.length != 9) {
                        continue;
                    }

                    Game game = new Game();
                    game.setId(Long.parseLong(fields[0]));
                    game.setGameNo(Integer.parseInt(fields[1]));
                    game.setGameName(fields[2]);
                    game.setGameCode(fields[3]);
                    game.setType(Integer.parseInt(fields[4]));
                    game.setCostPrice(Double.parseDouble(fields[5]));
                    game.setTax(Double.parseDouble(fields[6]));
                    game.setSalePrice(Double.parseDouble(fields[7]));
                    game.setDateOfSale(LocalDate.parse(fields[8]));

                    gamesBatch.add(game);

                    // When batchSize limit is reached, submit batch for saving
                    if (gamesBatch.size() >= batchSize) {
                        List<Game> batchToSave = new ArrayList<>(gamesBatch);
                        executor.submit(() -> saveBatch(batchToSave));
                        gamesBatch.clear();
                    }
                }

                // Submit the last batch if any records remain
                if (!gamesBatch.isEmpty()) {
                    executor.submit(() -> saveBatch(new ArrayList<>(gamesBatch)));
                }
            }

            executor.shutdown();

            // Wait for all threads to complete processing
            while (!executor.isTerminated()) {
                Thread.sleep(10);
            }

            // Record end time and calculate duration
            Instant end = Instant.now();
            long timeElapsed = Duration.between(start, end).toMillis();
            logger.info("Time taken to import and save all records: " + timeElapsed + " ms");

        } catch (Exception e) {
            logger.error("Failed to process file: {}", e.getMessage(), e);
        }
    }

    @Transactional
    private void saveBatch(List<Game> games) {
        try {
            gameRepository.saveAll(games);
        } catch (Exception e) {
            logger.error("Failed to save batch: {}", e.getMessage(), e);
        }
    }

}