package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class JasminTest {

    @Test
    public void ollirToJasminBasic() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminBasic.ollir");
    }

    @Test
    public void ollirToJasminArithmetics() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminArithmetics.ollir");
    }

    @Test
    public void ollirToJasminInvoke() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminInvoke.ollir");
    }

    @Test
    public void ollirToJasminFields() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminFields.ollir");
    }

    public static void testOllirToJasmin(String resource, String expectedOutput) {
        // If AstToJasmin pipeline, do not execute test
        if (TestUtils.hasAstToJasminClass()) {
            return;
        }

        var ollirResult = new OllirResult(SpecsIo.getResource(resource), Collections.emptyMap());

        var result = TestUtils.backend(ollirResult);

        var testName = new File(resource).getName();
        System.out.println(testName + ":\n" + result.getJasminCode());
        var runOutput = result.runWithFullOutput();
        assertEquals("Error while running compiled Jasmin: " + runOutput.getOutput(), 0, runOutput.getReturnValue());
        System.out.println("\n Result: " + runOutput.getOutput());

        if (expectedOutput != null) {
            assertEquals(expectedOutput, runOutput.getOutput());
        }
    }

    public static void testOllirToJasmin(String resource) {
        testOllirToJasmin(resource, null);
    }
}
