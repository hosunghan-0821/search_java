package search.discord;

import search.analyzer.CompareDataResult;
import search.analyzer.CompareStandard;
import search.kream.SearchProduct;
import search.kream.KreamSearchCore;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

import static search.discord.DiscordString.KREAM_COMPARE_CHANNEL;


@RequiredArgsConstructor
@Component
public class BotCommands extends ListenerAdapter {


    public static final String COMMAND_OPTIONS_PRODUCT_SKU = "상품_품번";
    public static final String COMMAND_OPTIONS_PRODUCT_PRICE = "상품_가격";

    public static final String COMMAND_OPTIONS_IS_FTA_PRODUCT = "fta_해당_품목";

    public static final String COMMAND_OPTIONS_PRODUCT_PRICE_UNIT = "상품_가격_단위";

    public static final String KREAM_ANALYZER = "kream-analyzer";

    private final CompareStandard compareStandard;

    private KreamSearchCore kreamSearchCore;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        final TextChannel textChannel = event.getJDA().getChannelById(TextChannel.class, KREAM_COMPARE_CHANNEL);
        assert (textChannel != null);

        if (event.getName().equals(KREAM_ANALYZER)) {
            OptionMapping skuOption = event.getOption(COMMAND_OPTIONS_PRODUCT_SKU);

            assert skuOption != null;
            String skuString = skuOption.getAsString();

            OptionMapping priceOption = event.getOption(COMMAND_OPTIONS_PRODUCT_PRICE);

            assert priceOption != null;
            String priceString = priceOption.getAsString();

            OptionMapping unitOption = event.getOption(COMMAND_OPTIONS_PRODUCT_PRICE_UNIT);
            String unit = "EURO";
            if (unitOption != null) {
                unit = unitOption.getAsString();
            }

            OptionMapping ftaOption = event.getOption(COMMAND_OPTIONS_IS_FTA_PRODUCT);
            String isFtaString = ftaOption.getAsString();

            String[] skuArray = skuString.split("/");
            String[] priceArray = priceString.split("/");
            String[] isFtaArray = isFtaString.split("/");

            if (!(skuArray.length == priceArray.length && priceArray.length == isFtaArray.length)) {
                event.reply("요청하신 상품 정보 옵션들의 개수가 일치하지 않습니다. 확인 부탁드립니다.").setEphemeral(true).queue();
                return;
            }
            event.reply("크림 분석시작합니다. 곧 결과를 알려드릴게요").setEphemeral(true).queue();

            for (int i = 0; i < skuArray.length; i++) {

                String sku = skuArray[i].strip();
                double inputPrice = Double.parseDouble(priceArray[i].strip().replaceAll(",", ""));
                boolean isFta = Boolean.parseBoolean(isFtaArray[i].strip());
                SearchProduct searchProduct = SearchProduct.builder()
                        .sku(sku)
                        .originSku(sku)
                        .originColorCode("null")
                        .inputPrice(inputPrice)
                        .unit(unit)
                        .fta(isFta)
                        .build();


                SearchProduct resultProduct = kreamSearchCore.searchProductOrNull(searchProduct);
                //상품 검색결과 없을 때
                if (!isKreamProductExist(resultProduct, textChannel, sku)) {
                    continue;
                }

                CompareDataResult compareDataResult = kreamSearchCore.compareProduct(resultProduct);
                //크림 가격 분석 및 이미지
                sendSearchAndCompareReport(textChannel, resultProduct, compareDataResult);
            }


        }

    }

    private boolean isKreamProductExist(SearchProduct resultProduct, TextChannel textChannel, String sku) {

        if (resultProduct == null) {
            textChannel.sendMessage(sku + " 품번을 확인해주세요").queue();
            return false;
        } else if (resultProduct.getTradingVolume() == null) {
            textChannel.sendMessage(sku + " 크림 거래량이 없습니다 확인해주세요").queue();
            return false;
        }
        return true;
    }

    public void sendSearchAndCompareReport(TextChannel textChannel, SearchProduct searchProduct, CompareDataResult compareDataResult) {


        // Embed 생성
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("비교 결과 리포트");
        embed.setDescription(
                "### 예상 수익률 : " + compareDataResult.getDifferenceRate() + "%" + "\n\n" +
                        "상품품번 : " + searchProduct.getOriginSku() + " " + searchProduct.getOriginColorCode() + "\n" +

                        "크림 모델번호 : " + searchProduct.getKreamModelNum() + "\n" +
                        "크림 상품명 : " + searchProduct.getName() + "\n" +
                        "크림 검색어 : " + searchProduct.getSku() + "\n" +
                        "모니터링 사이트 : " + searchProduct.getMonitoringSite() + "\n" +
                        "상품원산지 : " + searchProduct.getMadeBy() + "\n\n" +


                        "크림 총 거래량 : " + searchProduct.getTradingVolume() + "\n" +
                        "크림 즉시 구매가 : " + searchProduct.getInstantBuyPrice() + "\n" +
                        "크림 즉시 판매가 : " + searchProduct.getInstantSalePrice() + "\n" +
                        "크림 최근 평균 판매가 : " + getFormattedNumberString(searchProduct.getAveragePrice()) + "\n" +
                        "상품 매입가 : " + getFormattedNumberString(compareDataResult.getFinalPrice()) + "\n" +
                        "상품 해외 FinalPrice : " + searchProduct.getOriginPrice()+ "\n" +

                        "매입 합격 여부 : " + compareDataResult.isPassStandard() + "\n" +
                        "FTA 적용여부 : " + compareDataResult.isFtaProduct() + "\n " +
                        "설정한 환율 : " + compareDataResult.getUnitValue() + "\n\n"

        );

        if (compareDataResult.isPassStandard()) {
            embed.setColor(Color.GREEN); // Embed 색상 설정
        } else {
            embed.setColor(Color.RED);
        }

        embed.addField("사이트 바로가기", "[상품링크](" + searchProduct.getProductLink() + ")", false); // false는 필드가 인라인으로 표시되지 않도록 설정합니다.

        // 이미지 추가
        embed.setImage(searchProduct.getKreamImageUrl()); // 웹 이미지 사용
        textChannel.sendMessageEmbeds(embed.build()).queue();
        if (searchProduct.getProductLink() != null) {
            textChannel.sendMessage(searchProduct.getProductLink()).queue();
        }

    }

    public void setKreamSearchCore(KreamSearchCore kreamSearchCore) {
        this.kreamSearchCore = kreamSearchCore;
    }

    public String updateAnalyzerSettingOrNull(String message) {
        message = message.substring(1);
        String returnMessage = null;
        if (message.contains("환율")) {
            String[] split = message.split(" ");
            if (split.length == 2) {
                String changeUnitValue = split[1];
                compareStandard.setUnitValue(Double.parseDouble(changeUnitValue));
                returnMessage = "환율이 변경되었습니다.";
            }
        } else if (message.contains("수익률")) {
            String[] split = message.split(" ");
            if (split.length == 2) {
                String changeRate = split[1];
                compareStandard.setStandardRate(Double.parseDouble(changeRate));
                returnMessage = "목표 수익률 변경되었습니다.";
            }
        } else if (message.contains("status")) {
            returnMessage = "환율 : " + compareStandard.getUnitValue() + " 목표 수익률 " + compareStandard.getStandardRate();
        }

        return returnMessage;
    }


    private String getFormattedNumberString(double averagePrice) {
        NumberFormat format = NumberFormat.getInstance(Locale.US);
        String averagePriceS = format.format(averagePrice);
        averagePriceS += "원";
        return averagePriceS;
    }


}
