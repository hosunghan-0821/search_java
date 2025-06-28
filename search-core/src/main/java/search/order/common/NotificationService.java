package search.order.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.discord.DiscordBot;
import module.discord.DiscordString;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import search.controller.autoorder.dto.AutoOrderRequestDto;

import java.awt.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final Environment env;
    private final DiscordBot discordBot;


    /**
     * Discord Bot 알림을 위한 공통 메소드
     */
    public void sendAutoOrderNotification(String title, AutoOrderRequestDto dto, String errorMessage, Color color, String sendType, String... skus

    ) {
        Long discordChannel = null;

        boolean isDev = env.acceptsProfiles(Profiles.of("dev"));   // ★ 현재 dev?

        if (isDev) {
            discordChannel = DiscordString.GNB_TEST_ORDER_LOG_CHANNEL;
        } else {
            switch (sendType) {
                case "SUCCESS":
                    discordChannel = DiscordString.GNB_AUTO_ORDER_CHANNEL;
                    break;
                case "FAIL":
                    discordChannel = DiscordString.GNB_AUTO_ORDER_FAIL_CHANNEL;
                    break;
                case "LOG":
                    discordChannel = DiscordString.GNB_AUTO_ORDER_LOG_CHANNEL;
                    break;
                default:
                    break;
            }
        }


        try {
            discordBot.sendAutoOrderMessage(discordChannel, title, makeDiscordSendMessage(dto, title, errorMessage), dto.getProductLink(), skus, color);
        } catch (Exception e) {
            log.error("DISCORD SNED ERROR:  MESSAGE TITLE: {}", title);
        }

    }

    private String makeDiscordSendMessage(AutoOrderRequestDto autoOrderRequestDto, String headerMessage, String errorMessage) {
        return String.format("%s%n" + "sku                : %s%n" + "오류 메시지        : %s", headerMessage, autoOrderRequestDto.getSku(), errorMessage);
    }
}
