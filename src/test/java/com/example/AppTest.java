package com.example;    

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;    

public class AppTest {
    @Test
    public void testGetGreeting() {
        App app = new App();
        String greeting = app.getGreeting();
        assertEquals("Hello, World!", greeting);
    }
}