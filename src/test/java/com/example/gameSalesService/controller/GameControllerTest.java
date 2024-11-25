package com.example.gameSalesService.controller;

import com.example.gameSalesService.entity.Game;
import com.example.gameSalesService.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

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
        // Arrange: Mock the repository to return a list of games with "GameA"
        Game gameA = new Game();
        gameA.setGameName("GameA");

        List<Game> games = Collections.singletonList(gameA);
        given(gameRepository.findAll()).willReturn(games);

        // Act & Assert
        mockMvc.perform(get("/api/getGameSales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].gameName", is("GameA")));
    }

    @Test
    public void shouldReturnEmptyListWhenNoGameSalesAvailable() throws Exception {
        // Arrange: Mock the repository to return an empty list
        List<Game> games = Collections.emptyList();
        given(gameRepository.findAll()).willReturn(games);

        // Act & Assert
        mockMvc.perform(get("/api/getGameSales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // Check that the result is an empty list
    }

}
