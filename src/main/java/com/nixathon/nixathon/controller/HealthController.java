package com.nixathon.nixathon.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/healthz")
  public Map<String, String> test() {
    return Map.of("status", "OK");
  }
}
