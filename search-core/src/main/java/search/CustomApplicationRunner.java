package search;


import search.chrome.ChromeDriverTool;
import search.discord.DiscordBot;
import search.kream.KreamSearchCore;
import search.order.gnb.GnbOrderService;
import search.pool.SeleniumDriverPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;


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
        discordBot.getBotCommands().setKreamSearchCore(kreamSearchCore);

        seleniumDriverPool.initBrandSeleniumDriver("GNB", 5);
        BlockingQueue<ChromeDriverTool> gnbInitQueue = seleniumDriverPool.getBrandBlockingQueue("GNB");

        for (ChromeDriverTool chromeDriverTool : gnbInitQueue) {

            ChromeDriver driver = chromeDriverTool.getChromeDriver();
            WebDriverWait wait = chromeDriverTool.getWebDriverWait();
            gnbOrderService.login(driver, wait);
            chromeDriverTool.isReady(true);
        }

    }
}
