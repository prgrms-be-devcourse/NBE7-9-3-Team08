package com.backend.domain.analysis.controller;

import com.backend.domain.analysis.dto.response.AnalysisResultResponseDto;
import com.backend.domain.analysis.dto.response.HistoryResponseDto;
import com.backend.domain.analysis.service.AnalysisProgressService;
import com.backend.domain.analysis.service.AnalysisService;
import com.backend.domain.repository.dto.response.RepositoryComparisonResponse;
import com.backend.domain.repository.dto.response.RepositoryResponse;
import com.backend.domain.repository.service.RepositoryService;
import com.backend.domain.user.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private RepositoryService repositoryService;

    @MockitoBean
    private AnalysisProgressService analysisProgressService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserId(any(HttpServletRequest.class))).thenReturn(1L);
    }

    @Test
    @DisplayName("POST /api/analysis - 분석 요청 성공")
    void analyzeRepository_success() throws Exception {
        String url = "https://github.com/test/repo";

        when(analysisService.analyze(eq(url), any(HttpServletRequest.class)))
                .thenReturn(99L);

        String json = """
                { "githubUrl": "%s" }
                """.formatted(url);

        mockMvc.perform(
                        post("/api/analysis")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.repositoryId").value(99L));
    }

    @Test
    @DisplayName("GET /api/analysis/repositories - 사용자 저장소 목록 조회")
    void getMemberHistory_success() throws Exception {

        RepositoryResponse r = org.mockito.Mockito.mock(RepositoryResponse.class);
        when(repositoryService.getUserRepositories(any(HttpServletRequest.class)))
                .thenReturn(List.of(r));

        mockMvc.perform(
                        get("/api/analysis/repositories")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/analysis/repositories/{id} - 특정 저장소 분석 히스토리 조회")
    void getAnalysisByRepositoriesId_success() throws Exception {

        HistoryResponseDto dto = org.mockito.Mockito.mock(HistoryResponseDto.class);

        when(analysisService.getHistory(eq(1L), any(HttpServletRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(get("/api/analysis/repositories/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("GET /api/analysis/repositories/{repoId}/results/{analysisId} - 분석 상세 조회")
    void getAnalysisDetail_success() throws Exception {

        AnalysisResultResponseDto dto = org.mockito.Mockito.mock(AnalysisResultResponseDto.class);

        when(analysisService.getAnalysisDetail(eq(1L), eq(2L), any(HttpServletRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(
                        get("/api/analysis/repositories/1/results/2")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("DELETE /api/analysis/{userId}/repositories/{repoId} - 저장소 삭제")
    void deleteRepository_success() throws Exception {
        mockMvc.perform(
                        delete("/api/analysis/1/repositories/2")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("DELETE /api/analysis/{userId}/repositories/{repoId}/results/{analysisId} - 분석 결과 삭제")
    void deleteAnalysisResult_success() throws Exception {
        mockMvc.perform(
                        delete("/api/analysis/1/repositories/2/results/3")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("PUT /api/analysis/{userId}/repositories/{repoId}/public - 공개 여부 변경")
    void updatePublicStatus_success() throws Exception {
        mockMvc.perform(
                        put("/api/analysis/1/repositories/2/public")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/analysis/stream/{userId} - SSE 연결 성공")
    void stream_success() throws Exception {

        when(analysisProgressService.connect(eq(1L), any(HttpServletRequest.class)))
                .thenReturn(new SseEmitter(Long.MAX_VALUE));

        mockMvc.perform(get("/api/analysis/stream/1"))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/analysis/comparison - 비교 목록 조회")
    void getRepositoriesForComparison_success() throws Exception {

        RepositoryComparisonResponse r = org.mockito.Mockito.mock(RepositoryComparisonResponse.class);

        when(analysisService.getRepositoriesForComparison(any(HttpServletRequest.class)))
                .thenReturn(List.of(r));

        mockMvc.perform(
                        get("/api/analysis/comparison")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
