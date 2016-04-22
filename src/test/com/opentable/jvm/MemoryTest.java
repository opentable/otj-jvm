import org.junit.Assert;
import org.junit.Test;

import com.opentable.jvm.Memory;

public class MemoryTest {
    @Test
    public void nmt() {
        final String out = Memory.formatNmt();
        Assert.assertNotNull(out);
        System.out.print(out);
    }
}
