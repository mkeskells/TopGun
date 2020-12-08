package topgun.cmdline;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.util.Arrays;

public class TestFileParser {

    String[] param = null;
    JfrParseCommandLine options = null;
    CmdLineParser parser = null;
    Configuration configuration = null;


    @Before
    public void setup(){
        param = new String[]{
                "--jfr",
                "C:/Users/Dufie/Desktop/TopGun-Enhancement/jfr_files/flight_recording_1109comtradeenginetradeengineTradeEngineApplication12382.jfr",
                "--outDir",
                "C:/Users/Dufie/Documents/",
                "--userCodePackagePrefix",
                "com.tradeengine.trade.engine"
        };
        options = new JfrParseCommandLine();
        parser = new CmdLineParser(options);
        try {
            parser.parseArgument(param);

        } catch (CmdLineException e) {
            e.printStackTrace();
            return;
        }
        options.validate(parser);
        configuration = new Configuration((options));
    }

    @After
    public void cleanUp(){

    }


    @Test
    public void test_parse_method(){

        Totals totals = new Totals();
        new FileParser(options.jfr(),options,totals,configuration).parse();
        Assert.assertEquals(38,totals.consumedCpuEvents().get());

    }
}
