package com.example.gameSalesService.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

public class GameSalesCsvGenerator {

    private static final Logger logger = LoggerFactory.getLogger(GameSalesCsvGenerator.class);

    public static final int RECORD_COUNT = 1_000_000;

    public static void main(String[] args) {
        generateCsvFile("src/main/resources/game_sales_records.csv", RECORD_COUNT);
    }

    public static void generateCsvFile(String filePath, int recordCount) {
        String[] gameNames = {"SuperFun", "MegaGame", "BattleQuest", "SkyWarrior", "MysticLand", "RacingPro", "PuzzleMaster", "ArcadeChamp", "DungeonExplorer", "HeroSaga"};
        String[] gameCodes = {"SG1", "MG2", "BQ3", "SW4", "ML5", "RP6", "PM7", "AC8", "DE9", "HS0"};
        Random random = new Random();

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write the CSV header
            writer.append("id,game_no,game_name,game_code,type,cost_price,tax,sale_price,date_of_sale\n");

            for (int i = 1; i <= recordCount; i++) {
                int gameNo = random.nextInt(100) + 1;
                String gameName = gameNames[random.nextInt(gameNames.length)];
                String gameCode = gameCodes[random.nextInt(gameCodes.length)];
                int type = random.nextBoolean() ? 1 : 2;
                double costPrice = Math.round((random.nextDouble() * 100) * 100.0) / 100.0; // Cost price not more than 100, rounded to 2 decimal places
                double tax = Math.round((costPrice * 0.09) * 100.0) / 100.0; // 9% tax
                double salePrice = Math.round((costPrice + tax) * 100.0) / 100.0; // Sale price inclusive of tax
                // Generate a random date between April 1st and April 30th of the current year
                LocalDateTime dateOfSale = LocalDate.of(2024, 4, 1)
                        .atStartOfDay()
                        .plusDays(random.nextInt(30));

                // Write each record to CSV
                writer.append(String.format("%d,%d,%s,%s,%d,%.2f,%.2f,%.2f,%s\n",
                        i, gameNo, gameName, gameCode, type, costPrice, tax, salePrice, dateOfSale));
            }

            logger.info("CSV file created successfully at {}", filePath);

        } catch (IOException e) {
            logger.error("Error occurred while writing the CSV file: {}", e.getMessage(), e);
        }
    }
}
