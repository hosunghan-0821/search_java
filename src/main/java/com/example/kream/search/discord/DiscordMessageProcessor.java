package com.example.kream.search.discord;



import com.example.kream.search.chrome.ChromeDriverTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscordMessageProcessor {

    public String responseServerRunning(String monitoring,String message, ChromeDriverTool chromeDriverTool){
        message = message.substring(1);
        String returnMessage = "";
        if (message.equals("on")) {
            chromeDriverTool.isRunning(true);
            returnMessage = monitoring + " turn on Monitoring" ;
        } else if (message.equals("status")) {
            returnMessage = monitoring + "is now Running " + chromeDriverTool.isRunning();
        } else if (message.equals("off")) {
            chromeDriverTool.isRunning(false);
            returnMessage = monitoring + " turn off Monitoring ";
        }

        return returnMessage;
    }
}
