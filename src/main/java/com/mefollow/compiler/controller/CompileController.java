package com.mefollow.compiler.controller;

import com.mefollow.compiler.domain.CompileData;
import com.mefollow.compiler.service.CompileService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
public class CompileController {

    private final CompileService compileService;

    public CompileController(CompileService compileService) {
        this.compileService = compileService;
    }

    @GetMapping("/")
    public String redirectToMainPage() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String getMainPage() {
        return "main";
    }

//    @PostMapping("/compile")
//    @ResponseBody
//    public Mono<String> compileCodeAndGetResultPage(@RequestParam(name = "code") String code,
//                                                    @RequestParam(name = "inputParams") String inputParams) {
//        return compileService.compileCode(code, inputParams);
//    }

    @PostMapping("/compile")
    @ResponseBody
    public Mono<String> compileCodeAndGetResultPage(@RequestBody CompileData payload) {
        return compileService.compileCode(payload);
    }
}
