package jmri.jmrix.can.cbus.simulator;

import jmri.util.JUnitUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 * @author Steve Young Copyright (C) 2019
 */
public class CbusDummyNodeCanListenerTest {

    @Test
    public void testCTor() {
        CbusDummyNodeCanListener t = new CbusDummyNodeCanListener(null,null);
        Assert.assertNotNull("exists",t);
    }
    
    // The minimal setup for log4J
    @Before
    public void setUp() {
        JUnitUtil.setUp();
    }

    @After
    public void tearDown() {
        JUnitUtil.tearDown();
    }

    // private final static Logger log = LoggerFactory.getLogger(CbusNodeEventTest.class);

}
