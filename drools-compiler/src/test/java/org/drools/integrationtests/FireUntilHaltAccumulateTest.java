package org.drools.integrationtests;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.drools.ClockType;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.definition.type.FactType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.time.SessionPseudoClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests behavior of Drools Fusion when several events are being
 * fed into the engine by thread A while the engine had been started by
 * fireUntilHalt by thread B.
 */
public class FireUntilHaltAccumulateTest {

    private StatefulKnowledgeSession statefulSession;

    private StockFactory stockFactory;

    private static String drl = "package org.drools.integrationtests\n" +
                                "\n" +
                                "import java.util.List;\n" +
                                "\n" +
                                "declare Stock\n" +
                                "    @role( event )\n" +
                                "    @expires( 1s ) // setting to a large value causes the test to pass\n" +
                                "    name : String\n" +
                                "    value : Double\n" +
                                "end\n" +
                                "\n" +
                                "rule \"collect events\"\n" +
                                "when\n" +
                                "    stocks := List()\n" +
                                "        from accumulate( $zeroStock : Stock( value == 0.0 )\n" +
                                "                         collectList( $zeroStock ) )\n" +
                                "then\n" +
                                "end";

    @Before
    public void setUp() {
        final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newByteArrayResource( drl.getBytes() ), ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            System.err.println(kbuilder.getErrors().toString());
        }
        final KnowledgeBaseConfiguration config =
                KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);

        final KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase(config);
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());

        final KnowledgeSessionConfiguration sessionConfig =
                KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConfig.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));

        this.statefulSession = kbase.newStatefulKnowledgeSession(sessionConfig, null);
        this.stockFactory = new StockFactory(kbase);
    }

    @After
    public void cleanUp() {
        if (this.statefulSession != null) {
            this.statefulSession.dispose();
        }
    }

    /**
     * Events are being collected through accumulate while
     * separate thread inserts other events.
     * <p/>
     * The engine runs in fireUntilHalt mode started in a separate thread.
     * Events may expire during the evaluation of accumulate.
     */
    @Test
    public void testFireUntilHaltWithAccumulateAndExpires() throws Exception {
        // thread for firing until halt
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future sessionFuture = executor.submit(new Runnable() {

            public void run() {
                statefulSession.fireUntilHalt();
            }
        });

        try {
            for (int iteration = 0; iteration < 100; iteration++) {
                this.populateSessionWithStocks();
            }
            // let the engine finish its job
            Thread.sleep(2000);

        } finally {
            statefulSession.halt();
            // not to swallow possible exception
            sessionFuture.get();
        }
    }

    private void populateSessionWithStocks() {
        final SessionPseudoClock clock = statefulSession.getSessionClock();

        clock.advanceTime(1, TimeUnit.SECONDS);
        statefulSession.insert(stockFactory.createStock("ST1", 0d));
        clock.advanceTime(1, TimeUnit.SECONDS);
        statefulSession.insert(stockFactory.createStock("ST2", 1d));
        clock.advanceTime(1, TimeUnit.SECONDS);
        statefulSession.insert(stockFactory.createStock("ST3", 0d));
        clock.advanceTime(1, TimeUnit.SECONDS);
        statefulSession.insert(stockFactory.createStock("ST4", 0d));
        clock.advanceTime(1, TimeUnit.SECONDS);
        statefulSession.insert(stockFactory.createStock("ST5", 0d));
        clock.advanceTime(1, TimeUnit.SECONDS);
        statefulSession.insert(stockFactory.createStock("ST6", 1d));
    }

    /**
     * Factory creating events used in the test.
     */
    private static class StockFactory {

        private static final String DRL_PACKAGE_NAME = "org.drools.integrationtests";

        private static final String DRL_FACT_NAME = "Stock";

        private final KnowledgeBase kbase;

        public StockFactory(final KnowledgeBase kbase) {
            this.kbase = kbase;
        }

        public Object createStock(final String name, final Double value) {
            try {
                return this.createDRLStock(name, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to create Stock instance defined in DRL", e);
            } catch (InstantiationException e) {
                throw new RuntimeException("Unable to create Stock instance defined in DRL", e);
            }
        }

        private Object createDRLStock(final String name, final Double value)
                throws IllegalAccessException, InstantiationException {

            final FactType stockType = kbase.getFactType(DRL_PACKAGE_NAME, DRL_FACT_NAME);

            final Object stock = stockType.newInstance();
            stockType.set(stock, "name", name);
            stockType.set(stock, "value", value);

            return stock;
        }
    }
}