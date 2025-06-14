package search.common.log;


import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class BufferedLoggingAspect {

    @Around("@annotation(Buffered)")
    public Object bufferAndFlush(ProceedingJoinPoint pjp) throws Throwable {
        try (BufferedLogContext log =
                     BufferedLogContext.forClass(pjp.getTarget().getClass())) {
            return pjp.proceed();
        }
    }
}
