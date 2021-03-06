package org.drools.integrationtests;

import org.drools.Address;
import org.drools.CommonTestMethodBase;
import org.drools.KnowledgeBase;
import org.drools.Person;
import org.drools.runtime.StatefulKnowledgeSession;
import org.junit.Ignore;
import org.junit.Test;

public class NullSafeDereferencingTest extends CommonTestMethodBase {

    @Test
    public void testNullSafeBinding() {
        String str = "import org.drools.*;\n" +
                "rule R1 when\n" +
                "   Person( $streetName : address!.street ) \n" +
                "then\n" +
                "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        ksession.insert(new Person("Mario", 38));

        Person mark = new Person("Mark", 37);
        mark.setAddress(new Address("Main Street"));
        ksession.insert(mark);

        Person edson = new Person("Edson", 34);
        edson.setAddress(new Address(null));
        ksession.insert(edson);

        assertEquals(2, ksession.fireAllRules());
        ksession.dispose();
    }

    @Test
    public void testNullSafeNullComparison() {
        String str = "import org.drools.*;\n" +
                "rule R1 when\n" +
                "   Person( address!.street == null ) \n" +
                "then\n" +
                "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        ksession.insert(new Person("Mario", 38));

        Person mark = new Person("Mark", 37);
        mark.setAddress(new Address("Main Street"));
        ksession.insert(mark);

        Person edson = new Person("Edson", 34);
        edson.setAddress(new Address(null));
        ksession.insert(edson);

        assertEquals(1, ksession.fireAllRules());
        ksession.dispose();
    }

    @Test
    public void testNullSafeNullComparison2() {
        String str = "import org.drools.*;\n" +
                     "rule R1 when\n" +
                     " $street : String()\n"+
                     // this does not work
                     " Person( address!.street == $street ) \n" +
                     // this does
                     //" Person( address != null, address.street == $street ) \n" +
                     "then\n" +
                     "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        ksession.insert(new Person("Mario", 38));

        ksession.insert("Main Street");
        Person mark = new Person("Mark", 37);
        mark.setAddress(new Address("Main Street"));
        ksession.insert(mark);

        Person edson = new Person("Edson", 34);
        edson.setAddress(new Address(null));
        ksession.insert(edson);

        assertEquals(1, ksession.fireAllRules());
        ksession.dispose();
    }



    @Test
    public void testNullSafeNullComparisonReverse() {
        // DROOLS-82
        String str =
                "import org.drools.*;\n" +
                "rule R1 when\n" +
                " Person( \"Main Street\".equalsIgnoreCase(address!.street) )\n" +
                "then\n" +
                "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
        ksession.insert(new Person("Mario", 38));

        Person mark = new Person("Mark", 37);
        mark.setAddress(new Address("Main Street"));
        ksession.insert(mark);

        Person edson = new Person("Edson", 34);
        edson.setAddress(new Address(null));
        ksession.insert(edson);

        assertEquals(1, ksession.fireAllRules());
        ksession.dispose();
    }

    @Test
    public void testNullSafeNullComparisonReverseComplex() {
        // DROOLS-82
        String str =
                "import org.drools.*;\n" +
                "rule R1 when\n" +
                " Person( \"Main\".equalsIgnoreCase(address!.street!.substring(0, address!.street!.indexOf(' '))) )\n" +
                "then\n" +
                "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
        ksession.insert(new Person("Mario", 38));

        Person mark = new Person("Mark", 37);
        mark.setAddress(new Address("Main Street"));
        ksession.insert(mark);

        Person edson = new Person("Edson", 34);
        edson.setAddress(new Address(null));
        ksession.insert(edson);

        assertEquals(1, ksession.fireAllRules());
        ksession.dispose();
    }

    @Test
    public void testDoubleNullSafe() {
        String str = "import org.drools.*;\n" +
                "rule R1 when\n" +
                "   Person( address!.street!.length > 15 ) \n" +
                "then\n" +
                "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        ksession.insert(new Person("Mario", 38));

        Person mark = new Person("Mark", 37);
        mark.setAddress(new Address("Main Street"));
        ksession.insert(mark);

        Person edson = new Person("Edson", 34);
        edson.setAddress(new Address(null));
        ksession.insert(edson);

        Person alex = new Person("Alex", 34);
        alex.setAddress(new Address("The Main Very Big Street"));
        ksession.insert(alex);

        assertEquals(1, ksession.fireAllRules());
        ksession.dispose();
    }

    @Test
    public void testMixedNullSafes() {
        String str = "import org.drools.*;\n" +
                     "rule R1 when\n" +
                     "  $p : Person( " +
                     "               address!.street!.length > 0 && ( address!.street!.length < 15 || > 20 && < 30 ) " +
                     "               && address!.zipCode!.length > 0 && address.zipCode == \"12345\" " +
                     "  ) \n" +
                     "then\n" +
                     "  System.out.println( $p ); \n" +
                     "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        ksession.insert(new Person("Mario", 38));

        Person mark = new Person("Mark", 37);
        mark.setAddress(new Address("Main Street",null,"12345"));
        ksession.insert(mark);

        Person edson = new Person("Edson", 34);
        edson.setAddress(new Address(null));
        ksession.insert(edson);

        Person alex = new Person("Alex", 34);
        alex.setAddress(new Address("The Main Verrry Long Street"));
        ksession.insert(alex);

        Person frank = new Person("Frank", 24);
        frank.setAddress(new Address("Long Street number 21",null,"12345"));
        ksession.insert(frank);

        assertEquals(2, ksession.fireAllRules());
        ksession.dispose();
    }

    @Test
    public void testNullSafeMemberOf() {
        // DROOLS-50
        String str =
                "declare A\n" +
                "    list : java.util.List\n" +
                "end\n" +
                "\n" +
                "rule Init when\n" +
                "then\n" +
                "    insert( new A( java.util.Arrays.asList( \"test\" ) ) );" +
                "    insert( \"test\" );" +
                "end\n" +
                "rule R when\n" +
                "    $a : A()\n" +
                "    $s : String( this memberOf $a!.list )\n" +
                "then\n" +
                "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        assertEquals(2, ksession.fireAllRules());
        ksession.dispose();
    }

    @Test
    public void testNullSafeInnerConstraint() {
        String str =
                "declare Content\n" +
                "  complexContent : Content\n" +
                "  extension : Content\n" +
                "end\n" +
                "\n" +
                "declare Context\n" +
                "  ctx : Content\n" +
                "end\n" +
                "\n" +
                "rule \"Complex Type Attribute\"\n" +
                "when\n" +
                "    $con : Content()\n" +
                "    Context( ctx == $con || == $con!.complexContent!.extension )\n" +
                "then\n" +
                "   System.out.println( $con ); \n" +
                "end\n" +
                "\n" +
                "rule \"Init\"\n" +
                "when\n" +
                "then\n" +
                "    Content ext = new Content();\n" +
                "    Content complex = new Content( new Content( null, ext ), null );\n" +
                "    Content complex2 = new Content( null, null );\n" +
                "    Context ctx = new Context( ext );\n" +
                "    Context ctx2 = new Context( complex2 );\n" +
                "    insert( complex );\n" +
                "    insert( complex2 );\n" +
                "    insert( ctx );\n" +
                "    insert( ctx2 );\n" +
                "end";

        KnowledgeBase kbase = loadKnowledgeBaseFromString(str);
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        assertEquals( 3, ksession.fireAllRules() );
        ksession.dispose();
    }
}