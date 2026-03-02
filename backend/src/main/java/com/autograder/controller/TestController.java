package com.autograder.controller;

import com.google.gson.JsonObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping({"/test"})
    public JsonObject getTestObject() {
        JsonObject test = new JsonObject();
        test.addProperty("test", "If you see this in your browser, it works!");
        return test;
    }
}
