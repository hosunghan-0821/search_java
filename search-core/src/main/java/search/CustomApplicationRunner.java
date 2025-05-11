package search;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.discord.DiscordBot;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import search.chrome.ChromeDriverTool;
import search.kream.KreamSearchCore;
import search.order.gnb.GnbOrderService;
import search.pool.SeleniumDriverPool;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class CustomApplicationRunner implements ApplicationRunner {


    private final KreamSearchCore kreamSearchCore;

    private final DiscordBot discordBot;

    private final GnbOrderService gnbOrderService;

    private final SeleniumDriverPool seleniumDriverPool;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //기본 검색 크롬 만들어두기
//
//        chromeDriverToolFactory.makeChromeDriverTool(KREAM);
//
//        chromeDriverToolFactory.makeChromeDriverTool(BIFFI);
//        chromeDriverToolFactory.makeChromeDriverTool(DOBULE_F);

        //의존성 주입
//        discordBot.getBotCommands().setKreamSearchCore(kreamSearchCore);


        BlockingQueue<ChromeDriverTool> gnbInitQueue = seleniumDriverPool.getBrandBlockingQueue("GNB");

        List<CompletableFuture<Void>> futures = gnbInitQueue.stream()
                .map(chromeDriverTool -> CompletableFuture.runAsync(() -> {
                            try {
                                ChromeDriver driver = chromeDriverTool.getChromeDriver();
                                WebDriverWait wait = chromeDriverTool.getWebDriverWait();
                                // 로그인 시도
                                gnbOrderService.login(driver, wait);
                                // 준비 상태 표시
                                chromeDriverTool.isReady(true);
                            } catch (Exception e) {
                                log.error("초기 로그인 실패. 이후 보정으로 동작");
                            }
                        })
                )
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
