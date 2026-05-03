package com.example.backendastramaco.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void debeConectarConBaseDeDatosDePrueba() {
        assertNotNull(dataSource);
    }
}