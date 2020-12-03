package topgun.cmdline;

import java.io.PrintStream;
import java.io.PrintWriter;

public class JfrEventNotFoundException extends Exception{
    public JfrEventNotFoundException(String s) {
        super(s);
    }
}
