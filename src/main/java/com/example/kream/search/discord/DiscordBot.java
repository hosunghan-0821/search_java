package com.example.kream.search.discord;


import com.example.kream.search.chrome.ChromeDriverToolFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Slf4j
@RequiredArgsConstructor
@Component
public class DiscordBot extends ListenerAdapter {


    private final DiscordMessageProcessor discordMessageProcessor;

    private Map<String, String> channelHashMap = new HashMap<>();

    private ChromeDriverToolFactory chromeDriverToolFactory;
    private JDA jda;


    @Value("${discord.bot.token}")
    private String discordBotToken;

    @PostConstruct
    public void init() throws IOException {
        jda = JDABuilder.createDefault(discordBotToken)
                .setActivity(Activity.playing("서버 실행중"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<TextChannel> textChannels = jda.getTextChannels();
        //In-Memory로 들고 있는게 낫지 않은가?
        for (TextChannel textChannel : textChannels) {
            log.info("ID :" + textChannel.getId() + "NAME: " + textChannel.getName());
            channelHashMap.put(textChannel.getName(), textChannel.getId());
        }
    }

    public void setChromeDriverTool(ChromeDriverToolFactory chromeDriverToolFactory) {
        this.chromeDriverToolFactory = chromeDriverToolFactory;
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);

        User user = event.getAuthor();

        //로봇이 보낸건 무시.
        if (user.isBot() && !event.getMessage().toString().startsWith("!")) {
            return;
        }

        TextChannel textChannel = event.getChannel().asTextChannel();

        String channelName = textChannel.getName();
        String returnMessage = "";

    }


    public void sendMessage(String channelName, String message) {

        final String id = channelHashMap.get(channelName);
        final TextChannel textChannel = jda.getTextChannelById(id);
        if (textChannel != null) {
            textChannel.sendMessage(message).queue();
        } else {
            log.error("유효하지 않은 채널이름 : {}", channelName);
        }
    }






}
