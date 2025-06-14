package search.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class BufferedLogContext implements AutoCloseable{

    private static final int DEFAULT_LIMIT_BYTES = 32 * 1024; //32KB
    private static final ThreadLocal<StringBuilder> BUF = ThreadLocal.withInitial(() -> new StringBuilder(4096));

    /* ★ 현재 컨텍스트 기억용 */
    private static final ThreadLocal<BufferedLogContext> CURRENT = new ThreadLocal<>();


    private final Logger out;
    private final int limitBytes;
    private int currentBytes = 0;
    private boolean truncated = false;

    /* factory 메서드에서 CURRENT에 셋 */
    public static BufferedLogContext forClass(Class<?> owner) {
        BufferedLogContext ctx = new BufferedLogContext(LoggerFactory.getLogger(owner), DEFAULT_LIMIT_BYTES);
        CURRENT.set(ctx);
        return ctx;
    }
    public static BufferedLogContext forClass(Class<?> owner, int limitBytes) {
        BufferedLogContext ctx = new BufferedLogContext(LoggerFactory.getLogger(owner), limitBytes);
        CURRENT.set(ctx);
        return ctx;
    }
    /* 현재 컨텍스트 accessor */
    public static BufferedLogContext current() {
        return CURRENT.get();      // 없으면 null
    }

    private BufferedLogContext(Logger out, int limitBytes) {
        this.out = out;
        this.limitBytes = limitBytes;
    }

    public void info(String pattern, Object... args)  { append("INFO",  pattern, args); }
    public void warn(String pattern, Object... args)  { append("WARN",  pattern, args); }
    public void error(String pattern, Object... args) { append("ERROR", pattern, args); }

    private void append(String level, String pattern, Object... args) {
        if (truncated) return;

        String msg = MessageFormatter.arrayFormat(pattern, args).getMessage();
        int add = msg.length() + 1;                      // '\n'
        if (currentBytes + add > limitBytes) {
            BUF.get().append("... (truncated) ...\n");
            truncated = true;
            return;
        }
        BUF.get().append('[').append(level).append("] ")
                .append(msg).append('\n');
        currentBytes += add;
    }

    /* ---------- flush on close ---------- */

    @Override public void close() {
        String snapshot = BUF.get().toString();
        if (!snapshot.isEmpty()) {
            out.info("\n========== [{}] ==========\n{}",
                    Thread.currentThread().getName(), snapshot);
        }
        BUF.remove();
        CURRENT.remove();          // ★ 꼭 정리
    }
}
