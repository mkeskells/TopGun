package topgun.cmdline;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.*;

public class AggregateViewOptionHandler extends OptionHandler {
    public AggregateViewOptionHandler(CmdLineParser parser, OptionDef option, Setter<EnumSet<AggregateView>> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        int next = 0;

        EnumSet<AggregateView> result = EnumSet.noneOf(AggregateView.class);
        Set<String> all = new HashSet<>();
        EnumSet.allOf(AggregateView.class).stream().forEach(e -> all.add(e.toString()));

        while(params.size() > next && all.contains(params.getParameter(next))) {
            result.add(AggregateView.valueOf(params.getParameter(next)));
            next += 1;
        }
        setter.addValue(result);
        return next;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "aggregations";
    }
}
