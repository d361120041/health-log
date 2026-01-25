package tw.danielchiang.health_log.model.obj;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JoinObj {
    private final boolean isJoin;
    private final String join;
}
