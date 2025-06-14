package search.common.log;


import static java.util.Objects.nonNull;

/**
 * 얇은 헬퍼: {@link BufferedLogContext#current()} 를 안전하게 감싸서
 * null-check 레벨별 분기 없이 바로 호출할 수 있게 해 준다.
 * <p>
 * 사용 예)
 *
 * @Buffered void handle(Order o) {
 * BufferedLog.info("step-1 id={}", o.id());
 * ...
 * BufferedLog.warn("재시도 예정 orderId={}", o.id());
 * }
 */
public final class BufferedLog {        // ← 요청하신 이름

    private BufferedLog() {
    }            // static-only

    /* ---------------- public façade ---------------- */

    public static void info(String fmt, Object... args) {
        write(Level.INFO, fmt, args);
    }

    public static void warn(String fmt, Object... args) {
        write(Level.WARN, fmt, args);
    }

    public static void error(String fmt, Object... args) {
        write(Level.ERROR, fmt, args);
    }

    /* ---------------- internal ---------------- */

    private enum Level {INFO, WARN, ERROR}

    private static void write(Level lv, String fmt, Object... args) {
        BufferedLogContext ctx = BufferedLogContext.current();
        if (nonNull(ctx)) {                       // @Buffered 안 붙은 곳이면 noop
            switch (lv) {
                case INFO -> ctx.info(fmt, args);
                case WARN -> ctx.warn(fmt, args);
                case ERROR -> ctx.error(fmt, args);
            }
        }
    }
}