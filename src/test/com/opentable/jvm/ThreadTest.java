import org.junit.Assert;
import org.junit.Test;

import com.opentable.jvm.Thread;

public class ThreadTest {
    @Test
    public void info() {
        final String out = Thread.formatInfo();
        Assert.assertNotNull(out);
        System.out.print(out);
    }
}
