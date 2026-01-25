package tw.danielchiang.health_log.model.obj;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WhereObj {
    private final boolean isWhere;
    private final String column;
    private final Operator operator;
    private final String value;
    private final Logic logic;

    public enum Operator {
        EQ("="),
        NE("!="),
        GT(">"),
        GTE(">="),
        LT("<"),
        LTE("<="),
        LIKE("LIKE");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public enum Logic {
        AND("AND"),
        OR("OR");

        private final String symbol;

        Logic(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }
    
}
