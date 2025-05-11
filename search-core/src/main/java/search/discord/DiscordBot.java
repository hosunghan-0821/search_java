//package search.discord;
//
//
//import search.chrome.ChromeDriverToolFactory;
//import jakarta.annotation.PostConstruct;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.dv8tion.jda.api.EmbedBuilder;
//import net.dv8tion.jda.api.JDA;
//import net.dv8tion.jda.api.JDABuilder;
//import net.dv8tion.jda.api.entities.Activity;
//import net.dv8tion.jda.api.entities.Message;
//import net.dv8tion.jda.api.entities.User;
//import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
//import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
//import net.dv8tion.jda.api.hooks.ListenerAdapter;
//import net.dv8tion.jda.api.interactions.commands.OptionType;
//import net.dv8tion.jda.api.requests.GatewayIntent;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.awt.*;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static search.discord.BotCommands.*;
//
//
//@Slf4j
//@RequiredArgsConstructor
//@Component
//public class DiscordBot extends ListenerAdapter {
//
//
//    @Getter
//    private final BotCommands botCommands;
//    private Map<String, String> channelHashMap = new HashMap<>();
//
//    private ChromeDriverToolFactory chromeDriverToolFactory;
//    @Getter
//    private JDA jda;
//
//
//    @Value("${discord.bot.token}")
//    private String discordBotToken;
//
//    @PostConstruct
//    public void init() throws IOException {
//        jda = JDABuilder.createDefault(discordBotToken)
//                .setActivity(Activity.playing("서버 실행중"))
//                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
//                .addEventListeners(botCommands)
//                .addEventListeners(this)
//                .build();
//
//
//        try {
//            jda.awaitReady();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        jda.getGuildById(1220650964852408351L)
//                .upsertCommand(KREAM_ANALYZER, "kream price analyzer")
//                .addOption(OptionType.STRING, COMMAND_OPTIONS_PRODUCT_SKU, "크림에서 검색하려는 상품 품번 리스트", true)
//                .addOption(OptionType.STRING, COMMAND_OPTIONS_PRODUCT_PRICE, "상품 가격 리스트", true)
//                .addOption(OptionType.STRING, COMMAND_OPTIONS_IS_FTA_PRODUCT, "FTA 여부 리스트", true)
//                .queue();
//
//
//        List<TextChannel> textChannels = jda.getTextChannels();
//        //In-Memory로 들고 있는게 낫지 않은가?
//        for (TextChannel textChannel : textChannels) {
//            log.info("ID :" + textChannel.getId() + "NAME: " + textChannel.getName());
//            channelHashMap.put(textChannel.getName(), textChannel.getId());
//        }
//    }
//
//    public void setChromeDriverTool(ChromeDriverToolFactory chromeDriverToolFactory) {
//        this.chromeDriverToolFactory = chromeDriverToolFactory;
//    }
//
//
//    @Override
//    public void onMessageReceived(MessageReceivedEvent event) {
//        super.onMessageReceived(event);
//
//        User user = event.getAuthor();
//
//        TextChannel textChannel = event.getChannel().asTextChannel();
//        String message = event.getMessage().getContentDisplay();
//        String returnMessage = null;
//        Long channelIdLong = textChannel.getIdLong();
//        //로봇이 보낸건 무시.
//        if (user.isBot() && !event.getMessage().toString().startsWith("!")) {
//            return;
//        }
//        if (channelIdLong.equals(DiscordString.KREAM_COMPARE_CHANNEL)) {
//            if (message.startsWith("!")) {
//                returnMessage = botCommands.updateAnalyzerSettingOrNull(message);
//            }
//        }
//
//        if (returnMessage != null) {
//            textChannel.sendMessage(returnMessage).queue();
//        }
//
//
//    }
//
//
//    public void sendMessage(String channelName, String message) {
//
//        final String id = channelHashMap.get(channelName);
//        final TextChannel textChannel = jda.getTextChannelById(id);
//        if (textChannel != null) {
//            textChannel.sendMessage(message).queue();
//        } else {
//            log.error("유효하지 않은 채널이름 : {}", channelName);
//        }
//    }
//
//
//}
