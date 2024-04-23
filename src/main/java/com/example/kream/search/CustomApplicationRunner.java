package com.example.kream.search;


import com.example.kream.search.chrome.ChromeDriverTool;
import com.example.kream.search.chrome.ChromeDriverToolFactory;
import com.example.kream.search.discord.DiscordBot;
import com.example.kream.search.kream.CommonProduct;
import com.example.kream.search.kream.KreamMonitorCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.kream.search.kream.KreamString.KREAM;


@Slf4j
@Component
@RequiredArgsConstructor
public class CustomApplicationRunner implements ApplicationRunner {

    private final ChromeDriverToolFactory chromeDriverToolFactory;

    private final KreamMonitorCore kreamMonitorCore;

    private final DiscordBot discordBot;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //의존성 주입
        discordBot.getBotCommands().setKreamMonitorCore(kreamMonitorCore);
    }
}
