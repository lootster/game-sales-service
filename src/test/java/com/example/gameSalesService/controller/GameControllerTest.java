package com.example.gameSalesService.controller;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameRepository gameRepository;

    @Test
    public void shouldReturnStatusOkForHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnStatusOkForImportCsv() throws Exception {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "games.csv",
                "text/csv",
                """
                id,game_no,game_name,game_code,type,cost_price,tax,sale_price,date_of_sale
                1,10,GameA,GA,1,50.0,4.5,54.5,2024-11-25T10:00:00
                2,15,GameB,GB,2,30.0,2.7,32.7,2024-11-25T11:00:00
                """.getBytes()
        );

        mockMvc.perform(multipart("/api/import")
                        .file(csvFile))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnGameSales() throws Exception {
        // Arrange: Mock the repository to return a page of games with "GameA"
        Game gameA = new Game();
        gameA.setGameName("GameA");

        List<Game> games = Collections.singletonList(gameA);
        Page<Game> gamePage = new PageImpl<>(games); // Mocking a Page object

        // Mock the Pageable argument
        given(gameRepository.findAll(PageRequest.of(0, 100))).willReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/getGameSales")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.games[0].gameName", is("GameA")));
    }

    @Test
    public void shouldReturnEmptyListWhenNoGameSalesAvailable() throws Exception {
        // Arrange: Mock the repository to return an empty page
        List<Game> games = Collections.emptyList();
        Page<Game> gamePage = new PageImpl<>(games); // Mocking an empty Page object

        // Mock the Pageable argument
        given(gameRepository.findAll(PageRequest.of(0, 100))).willReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/getGameSales")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(0))); // Check that the result is an empty list
    }

    @Test
    public void shouldReturnGameSalesWithinGivenPeriod() throws Exception {
        // Arrange: Mock the repository to return games within a specified date range
        Game gameA = new Game();
        gameA.setGameName("GameA");
        gameA.setDateOfSale(LocalDate.of(2024, 11, 25).atStartOfDay());

        Game gameB = new Game();
        gameB.setGameName("GameB");
        gameB.setDateOfSale(LocalDate.of(2024, 11, 26).atStartOfDay());

        List<Game> games = Arrays.asList(gameA, gameB);
        Page<Game> gamePage = new PageImpl<>(games);

        // Mock the Pageable argument and repository method for date range query
        Pageable pageable = PageRequest.of(0, 100);
        LocalDate fromDate = LocalDate.of(2024, 11, 24);
        LocalDate toDate = LocalDate.of(2024, 11, 27);

        // Mocking the repository to return gamePage when using the date range
        given(gameRepository.findAllByDateOfSaleBetween(fromDate, toDate, pageable)).willReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/getGameSales")
                        .param("fromDate", "2024-11-24")
                        .param("toDate", "2024-11-27")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(2))) // Checking the size of the game list
                .andExpect(jsonPath("$.games[0].gameName", is("GameA"))) // Validate game name
                .andExpect(jsonPath("$.games[1].gameName", is("GameB"))) // Validate game name
                .andExpect(jsonPath("$.currentPage", is(0))) // Check the current page
                .andExpect(jsonPath("$.totalItems", is(2))) // Check total items
                .andExpect(jsonPath("$.totalPages", is(1))); // Check total pages
    }

    @Test
    public void shouldReturnGameSalesWithSalePriceGreaterThanGivenValue() throws Exception {
        // Arrange: Mock the repository to return games with a sale price greater than the specified value
        Game gameA = new Game();
        gameA.setGameName("GameA");
        gameA.setSalePrice(200.0);

        Game gameB = new Game();
        gameB.setGameName("GameB");
        gameB.setSalePrice(300.0);

        List<Game> games = Arrays.asList(gameA, gameB);
        Page<Game> gamePage = new PageImpl<>(games);

        // Mock the Pageable argument and repository method for price range query
        Pageable pageable = PageRequest.of(0, 100);
        Double salePrice = 150.0;

        // Mocking the repository to return gamePage when sale price is greater than the given value
        given(gameRepository.findAllBySalePriceGreaterThan(salePrice, pageable)).willReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/getGameSales")
                        .param("salePrice", "150")
                        .param("filter", "greaterThan")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(2))) // Checking the size of the game list
                .andExpect(jsonPath("$.games[0].gameName", is("GameA"))) // Validate game name
                .andExpect(jsonPath("$.games[1].gameName", is("GameB"))); // Validate game name
    }

    @Test
    public void shouldReturnGameSalesWithSalePriceLessThanGivenValue() throws Exception {
        // Arrange: Mock the repository to return games with a sale price less than the specified value
        Game gameA = new Game();
        gameA.setGameName("GameA");
        gameA.setSalePrice(50.0);

        Game gameB = new Game();
        gameB.setGameName("GameB");
        gameB.setSalePrice(70.0);

        List<Game> games = Arrays.asList(gameA, gameB);
        Page<Game> gamePage = new PageImpl<>(games);

        // Mock the Pageable argument and repository method for price range query
        Pageable pageable = PageRequest.of(0, 100);
        Double salePrice = 100.0;

        // Mocking the repository to return gamePage when sale price is less than the given value
        given(gameRepository.findAllBySalePriceLessThan(salePrice, pageable)).willReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/getGameSales")
                        .param("salePrice", "100")
                        .param("filter", "lessThan")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(2))) // Checking the size of the game list
                .andExpect(jsonPath("$.games[0].gameName", is("GameA"))) // Validate game name
                .andExpect(jsonPath("$.games[1].gameName", is("GameB"))); // Validate game name
    }



}