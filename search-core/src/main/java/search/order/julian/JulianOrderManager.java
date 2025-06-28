package search.order.julian;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.database.repository.ProductRepository;
import module.discord.DiscordBot;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import search.order.common.index.TokenEvaluator;
import search.pool.SeleniumDriverPool;

import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Component
@Slf4j
public class JulianOrderManager {
    private final JulianOrderService julianOrderService;
    private final SeleniumDriverPool seleniumDriverPool;
    private final ProductRepository productRepository;
    private final TokenEvaluator tokenEvaluator;
    private final ReentrantLock finalOrderStepLock = new ReentrantLock(true);
    private final DiscordBot discordBot;
    private final Environment env;




}
