package com.exemple.the_shop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    public void ttt() {

    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
