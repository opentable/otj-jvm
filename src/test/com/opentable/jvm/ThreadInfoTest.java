import org.junit.Assert;
import org.junit.Test;

import com.opentable.jvm.ThreadInfo;

public class ThreadInfoTest {
    @Test
    public void info() {
        final String out = ThreadInfo.format();
        Assert.assertNotNull(out);
        System.out.print(out);
    }
}
