package com.example.gameSalesService.service;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.entity.GameSalesAggregated;
import com.example.gameSalesService.repository.GameRepository;
import com.example.gameSalesService.repository.GameSalesAggregatedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;

@Service
public class ImportService {

    private final Logger logger = LoggerFactory.getLogger(ImportService.class);

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameSalesAggregatedRepository gameSalesAggregatedRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Async
    public void processFileAsync(String filePath, Instant start) {
        try {
            int batchSize = 2000;
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Game> gamesBatch = new ArrayList<>();
            Map<LocalDate, GameSalesAggregated> aggregationMap = new ConcurrentHashMap<>();

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

                    // Aggregation logic
                    LocalDate saleDate = game.getDateOfSale();
                    double salePrice = game.getSalePrice();
                    Integer gameNo = game.getGameNo();

                    // Aggregate by date and game number
                    aggregationMap.compute(saleDate, (date, aggregated) -> {
                        if (aggregated == null) {
                            aggregated = new GameSalesAggregated();
                            aggregated.setDateOfSale(date);
                            aggregated.setTotalGamesSold(0);
                            aggregated.setTotalSales(0.0);
                            aggregated.setGameNo(gameNo);  // Set game number
                        }

                        aggregated.setTotalGamesSold(aggregated.getTotalGamesSold() + 1);
                        aggregated.setTotalSales(aggregated.getTotalSales() + salePrice);

                        return aggregated;
                    });

                    // Save the batch when batchSize is reached
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

            // Save aggregated data after processing all records
            saveAggregatedData(aggregationMap);

            // Record end time and calculate duration
            Instant end = Instant.now();
            long timeElapsed = Duration.between(start, end).toMillis();
            logger.info("Time taken to import and save all records: " + timeElapsed + " ms");

        } catch (Exception e) {
            logger.error("Failed to process file: {}", e.getMessage(), e);
        }
    }

    private void saveBatch(List<Game> games) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            for (Game game : games) {
                em.persist(game);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Failed to save batch: {}", e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    private void saveAggregatedData(Map<LocalDate, GameSalesAggregated> aggregationMap) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            for (GameSalesAggregated aggregated : aggregationMap.values()) {
                em.persist(aggregated);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Failed to save aggregated data: {}", e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
