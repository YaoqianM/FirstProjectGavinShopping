package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.service.AuditLogService;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import com.bfs.hibernateprojectdemo.service.ProductAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.bfs.hibernateprojectdemo.security.JwtAuthFilter;
import com.bfs.hibernateprojectdemo.security.JwtService;
import com.bfs.hibernateprojectdemo.service.RegisterService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductPatchConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionFactory sessionFactory;
    @MockBean
    private HomePageService homePageService;
    @MockBean
    private ProductAnalyticsService productAnalyticsService;
    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private RegisterService registerService;

    @Test
    void patchFailsWithEtagMismatch() throws Exception {
        Session s = Mockito.mock(Session.class);
        Transaction tx = Mockito.mock(Transaction.class);
        Mockito.when(sessionFactory.openSession()).thenReturn(s);
        Mockito.when(s.beginTransaction()).thenReturn(tx);

        Product p = new Product();
        // simulate version 1 and id 10
        java.lang.reflect.Field idField = Product.class.getDeclaredField("productId");
        idField.setAccessible(true);
        idField.set(p, 10L);
        java.lang.reflect.Field vField = Product.class.getDeclaredField("version");
        vField.setAccessible(true);
        vField.set(p, 1L);

        Mockito.when(s.get(Product.class, 10L)).thenReturn(p);

        String body = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"X\"}]";
        String wrongEtag = "W\"product-10-v0\""; // wrong version format intentionally

        mockMvc.perform(patch("/products/10")
                        .contentType("application/json-patch+json")
                        .content(body)
                        .header("If-Match", wrongEtag))
                .andExpect(status().isPreconditionFailed());
    }
}