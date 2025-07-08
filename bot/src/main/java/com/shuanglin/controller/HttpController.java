package com.shuanglin.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bot")
public class HttpController {

	@PostMapping("")
	public void post(@RequestBody String message) {
		System.out.println("message = " + message);
	}
}
