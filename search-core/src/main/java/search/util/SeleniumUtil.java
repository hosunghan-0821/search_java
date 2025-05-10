package search.util;

import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.chrome.ChromeDriver;

public class SeleniumUtil {

    /**
     * ChromeDriver 세션이 아직 살아 있는지 확인합니다.
     *
     * @param driver 확인할 ChromeDriver 인스턴스
     * @return 세션이 유효하면 true, 끊겼으면 false
     */
    public static boolean isSessionAlive(ChromeDriver driver) {
        try {
            // 아무 호출이나 하면 됩니다. 윈도우 핸들을 조회해도 되고...
            driver.getWindowHandle();
            return true;
        } catch (NoSuchSessionException e) {
            // 세션이 이미 종료된 경우
            return false;
        } catch (Exception e) {
            // 네트워크 단절 등으로 다른 WebDriverException 이 올 수도 있으니 포괄적으로 잡아도 됩니다.
            return false;
        }
    }
}
