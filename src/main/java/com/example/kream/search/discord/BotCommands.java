package com.example.kream.search.discord;

import com.example.kream.search.kream.CommonProduct;
import com.example.kream.search.kream.KreamMonitorCore;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class BotCommands extends ListenerAdapter {

    public static final String COMMAND_OPTIONS_PRODUCT_SKU ="상품_품번";
    public static final String COMMAND_OPTIONS_PRODUCT_PRICE ="상품_가격";

    public static final String COMMAND_OPTIONS_PRODUCT_PRICE_UNIT = "상품_가격_단위";

    public static final String KREAM_ANALYZER = "kream-analyzer";

    public KreamMonitorCore kreamMonitorCore;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {


        if(event.getName().equals(KREAM_ANALYZER)) {
            OptionMapping skuOption = event.getOption(COMMAND_OPTIONS_PRODUCT_SKU);

            assert skuOption != null;
            String sku = skuOption.getAsString();

            OptionMapping priceOption = event.getOption(COMMAND_OPTIONS_PRODUCT_PRICE);

            assert priceOption != null;
            double asDouble = priceOption.getAsDouble();

            OptionMapping unitOption = event.getOption(COMMAND_OPTIONS_PRODUCT_PRICE_UNIT);
            String unit = "유로";
            if(unitOption != null) {
                unit = unitOption.getAsString();
            }

            CommonProduct commonProduct = CommonProduct.builder()
                    .sku(sku)
                    .price(asDouble)
                    .unit(unit)
                    .build();

            event.reply("크림 분석시작합니다. 곧 결과를 알려드릴게요").queue();
            CommonProduct resultProduct = kreamMonitorCore.runProductCompareLogic(commonProduct);
            event.getChannel().asTextChannel().sendMessage(resultProduct.toString()).queue();


        }



    }
    public void setKreamMonitorCore(KreamMonitorCore kreamMonitorCore) {
        this.kreamMonitorCore = kreamMonitorCore;
    }
}
