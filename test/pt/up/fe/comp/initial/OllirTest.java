package pt.up.fe.comp.initial;

import org.junit.Test;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class OllirTest {


    @Test
    public void compileBasic() {
        testJmmCompilation("pt/up/fe/comp/initial/ollir/CompileBasic.jmm", this::compileBasic);
    }

    @Test
    public void compileArithmetic() {
        testJmmCompilation("pt/up/fe/comp/initial/ollir/CompileArithmetic.jmm", this::compileArithmetic);
    }

    @Test
    public void compileAssignment() {
        testJmmCompilation("pt/up/fe/comp/initial/ollir/CompileAssignment.jmm", this::compileAssignment);
    }

    public static void testJmmCompilation(String resource, Consumer<ClassUnit> ollirTester, String executionOutput) {

        // If AstToJasmin pipeline, generate Jasmin
        if (TestUtils.hasAstToJasminClass()) {

            var result = TestUtils.backend(SpecsIo.getResource(resource));

            var testName = new File(resource).getName();
            System.out.println(testName + ":\n" + result.getJasminCode());

            // Initial tests do not run code since the initial grammar does not support main methods
            return;
        }

        var result = TestUtils.optimize(SpecsIo.getResource(resource));
        var testName = new File(resource).getName();
        System.out.println(testName + ":\n" + result.getOllirCode());

        if (ollirTester != null) {
            ollirTester.accept(result.getOllirClass());
        }
    }

    public static void testJmmCompilation(String resource, Consumer<ClassUnit> ollirTester) {
        testJmmCompilation(resource, ollirTester, null);
    }

    public void compileBasic(ClassUnit classUnit) {

        // Test name of the class and super
        assertEquals("Class name not what was expected", "CompileBasic", classUnit.getClassName());

        // Test method 1
        Method method1 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("methodOne"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find methodOne", method1);

        var retInst1 = method1.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in methodOne", retInst1.isPresent());

        // Test method 2
        Method method2 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("methodTwo"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find methodTwo'", method2);

        var retInst2 = method2.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in methodTwo", retInst2.isPresent());
    }

    public void compileArithmetic(ClassUnit classUnit) {
        // Test name of the class
        assertEquals("Class name not what was expected", "CompileArithmetic", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var binOpInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof AssignInstruction)
                .map(instr -> (AssignInstruction) instr)
                .filter(assign -> assign.getRhs() instanceof BinaryOpInstruction)
                .findFirst();

        assertTrue("Could not find a binary op instruction in method " + methodName, binOpInst.isPresent());

        var retInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method " + methodName, retInst.isPresent());

    }


    public void compileAssignment(ClassUnit classUnit) {
        // Test name of the class
        assertEquals("Class name not what was expected", "CompileAssignment", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var assignInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof AssignInstruction)
                .map(AssignInstruction.class::cast)
                .findFirst();
        assertTrue("Could not find an assign instruction in method " + methodName, assignInst.isPresent());

        assertEquals("Assignment does not have the expected type", ElementType.INT32,
                assignInst.get().getTypeOfAssign().getTypeOfElement());
    }
}
