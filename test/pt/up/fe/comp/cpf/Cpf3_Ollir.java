/**
 * Copyright 2022 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.comp.cpf;

import org.junit.Test;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.CpUtils;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.stream.Collectors;

public class Cpf3_Ollir {

    static OllirResult getOllirResult(String filename) {
        return TestUtils.optimize(SpecsIo.getResource("pt/up/fe/comp/cpf/3_ollir/" + filename));
    }


    /*checks if method declaration is correct (array)*/
    @Test
    public void section1_Basic_Method_Declaration_Array() {
        var result = getOllirResult("basic/BasicMethodsArray.jmm");

        var method = CpUtils.getMethod(result, "func4");

        CpUtils.assertEquals("Method return type", "int[]", CpUtils.toString(method.getReturnType()), result);
    }


    @Test
    public void section2_Arithmetic_Simple_and() {
        var ollirResult = getOllirResult("arithmetic/Arithmetic_and.jmm");

        var method = CpUtils.getMethod(ollirResult, "main");

        // Check if it has ifs and a gotos
        var ifInsts = CpUtils.getInstructions(CondBranchInstruction.class, method);
        var gotoInsts = CpUtils.getInstructions(GotoInstruction.class, method);

        CpUtils.assertTrue("Expected to find 1 if in method " + method.getMethodName(), ifInsts.size() == 1, ollirResult);
        CpUtils.assertTrue("Expected to find 1 goto in method " + method.getMethodName(), gotoInsts.size() == 1, ollirResult);
    }

    @Test
    public void section2_Arithmetic_Simple_less() {
        var ollirResult = getOllirResult("arithmetic/Arithmetic_less.jmm");

        var method = CpUtils.getMethod(ollirResult, "main");

        // Check if it has either a LTH operation, a GTE operation, or two if/else
        var lthOps = CpUtils.getOperationInstances(OperationType.LTH, method, ollirResult);
        var gteOps = CpUtils.getOperationInstances(OperationType.GTE, method, ollirResult);

        var ifInsts = CpUtils.getInstructions(CondBranchInstruction.class, method);

        var validCode = lthOps.size() == 1 || gteOps.size() == 1 || ifInsts.size() == 2;

        CpUtils.assertTrue(
                "Could not find either an LTH or GTE operations, of two if/elses in method " + method.getMethodName(),
                validCode, ollirResult);

    }

    @Test
    public void section2_Arithmetic_not() {
        var ollirResult = getOllirResult("arithmetic/Arithmetic_not.jmm");

        var method = CpUtils.getMethod(ollirResult, "main");

        CpUtils.assertNumberOfOperations(OperationType.NOTB, 1, method, ollirResult);
    }


    @Test
    public void section3_ControlFlow_If_Simple_Single_goto() {

        var result = getOllirResult("control_flow/SimpleIfElseStat.jmm");

        var method = CpUtils.getMethod(result, "func");

        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Expected number of branches to be between 1 and 2", branches.size() >= 1 && branches.size() <= 2, result);

        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Has at least 1 goto", gotos.size() >= 1, result);
    }

    @Test
    public void section3_ControlFlow_If_Switch() {

        var result = getOllirResult("control_flow/SwitchStat.jmm");

        var method = CpUtils.getMethod(result, "func");

        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Expected number of branches to be between 6 and 12", branches.size() >= 6 && branches.size() <= 12, result);

        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Has at least 6 gotos", gotos.size() >= 6, result);
    }

    @Test
    public void section3_ControlFlow_While_Simple() {

        var result = getOllirResult("control_flow/SimpleWhileStat.jmm");

        var method = CpUtils.getMethod(result, "func");

        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);

        CpUtils.assertTrue("Number of branches between 1 and 3", branches.size() >= 1 && branches.size() <= 3, result);
    }

    @Test
    public void section3_ControlFlow_If_Else_In_Main() {
        var result = getOllirResult("control_flow/IfElseInMain.jmm");

        var method = CpUtils.getMethod(result, "main");

        var branches = CpUtils.assertInstExists(CondBranchInstruction.class, method, result);
        CpUtils.assertTrue("Expected number of branches to be between 1 and 2", branches.size() >= 1 && branches.size() <= 2, result);

        var gotos = CpUtils.assertInstExists(GotoInstruction.class, method, result);
        CpUtils.assertTrue("Has at least 1 goto", gotos.size() >= 1, result);

        // One way of ensuring that all labels always have a corresponding statement
        // is to ensure that all methods (including ones which return type is void)
        // have a return statement
        var returns = CpUtils.assertInstExists(ReturnInstruction.class, method, result);
        CpUtils.assertTrue("Has return", returns.size() == 1, result);
    }

    /*checks if an array is correctly initialized*/
    @Test
    public void section4_Arrays_New_Array() {
        var result = getOllirResult("arrays/ArrayNew.jmm");

        var method = CpUtils.getMethod(result, "main");

        var calls = CpUtils.assertInstExists(CallInstruction.class, method, result);

        CpUtils.assertEquals("Number of calls", 3, calls.size(), result);

        // Get new
        var newCalls = calls.stream().filter(call -> call.getInvocationType() == CallType.NEW)
                .collect(Collectors.toList());

        CpUtils.assertEquals("Number of 'new' calls", 1, newCalls.size(), result);

        // Get length
        var lengthCalls = calls.stream().filter(call -> call.getInvocationType() == CallType.arraylength)
                .collect(Collectors.toList());

        CpUtils.assertEquals("Number of 'arraylenght' calls", 1, lengthCalls.size(), result);
    }

    /*checks if the access to the elements of array is correct*/
    @Test
    public void section4_Arrays_Access_Array() {
        var result = getOllirResult("arrays/ArrayAccess.jmm");

        var method = CpUtils.getMethod(result, "foo");

        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);
        var numArrayStores = assigns.stream().filter(assign -> assign.getDest() instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array stores", 5, numArrayStores, result);

        var numArrayReads = assigns.stream()
                .flatMap(assign -> CpUtils.getElements(assign.getRhs()).stream())
                .filter(element -> element instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array reads", 5, numArrayReads, result);
    }

    /*checks multiple expressions as indexes to access the elements of an array*/
    @Test
    public void section4_Arrays_Load_ComplexArrayAccess() {
        // Just parse
        var result = getOllirResult("arrays/ComplexArrayAccess.jmm");

        System.out.println("---------------------- OLLIR ----------------------");
        System.out.println(result.getOllirCode());
        System.out.println("---------------------- OLLIR ----------------------");

        var method = CpUtils.getMethod(result, "main");

        var assigns = CpUtils.assertInstExists(AssignInstruction.class, method, result);
        var numArrayStores = assigns.stream().filter(assign -> assign.getDest() instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array stores", 5, numArrayStores, result);

        var numArrayReads = assigns.stream()
                .flatMap(assign -> CpUtils.getElements(assign.getRhs()).stream())
                .filter(element -> element instanceof ArrayOperand).count();
        CpUtils.assertEquals("Number of array reads", 6, numArrayReads, result);
    }

    @Test
    public void section4_Arrays_Varargs() {
        var result = getOllirResult("arrays/ArrayVarArgs.jmm");

        var method = CpUtils.getMethod(result, "bar");

        var calls = CpUtils.getInstructions(CallInstruction.class, method);

        var fooCalls = calls.stream()
                .filter(call -> call.getMethodNameTry().isPresent())
                .filter(call -> ((LiteralElement) call.getMethodName()).getLiteral().equals("\"foo\""))
                .toList();

        CpUtils.assertTrue("Expected two calls to method 'foo' in method 'bar'", fooCalls.size() == 2, result);

        for (var fooCall : fooCalls) {
            CpUtils.assertTrue("Expected a single argument in call to 'foo'", fooCall.getArguments().size() == 1, result);
            var arg = fooCall.getArguments().get(0);
            CpUtils.assertTrue("Expected single argument in call to 'foo' to be an array", arg.getType() instanceof ArrayType, result);
        }


        var assigns = CpUtils.getInstructions(AssignInstruction.class, method);
        CpUtils.assertTrue("Expected at least 6 assignments in method 'bar'", assigns.size() >= 6, result);

    }

    @Test
    public void section4_Arrays_Array_Initialization() {
        var result = getOllirResult("arrays/ArrayInitialization.jmm");

        var method = CpUtils.getMethod(result, "foo");

        var calls = CpUtils.getInstructions(CallInstruction.class, method);

        calls.stream().forEach(c -> System.out.println(c));

        var newCalls = calls.stream()
                .filter(call -> call.getInvocationType() == CallType.NEW)
                .toList();

        CpUtils.assertTrue("Expected one call to new in method 'foo'", newCalls.size() == 1, result);

        var newCall = newCalls.get(0);

        CpUtils.assertTrue("Expected one argument in call to new", newCall.getArguments().size() == 1, result);

        var arrayLength = newCall.getArguments().get(0);

        CpUtils.assertTrue("Expected argument to be a literal", arrayLength instanceof LiteralElement, result);
        CpUtils.assertTrue("Expected argument to have the value 4", ((LiteralElement) arrayLength).getLiteral().equals("4"), result);

        var assigns = CpUtils.getInstructions(AssignInstruction.class, method);
        CpUtils.assertTrue("Expected at least 6 assignments in method 'bar'", assigns.size() >= 6, result);
    }


    @Test
    public void section4_Arrays_VarargsAndArrayInit() {
        var result = getOllirResult("arrays/VarargsAndArrayInit.jmm");


        var method = CpUtils.getMethod(result, "bar");

        var calls = CpUtils.getInstructions(CallInstruction.class, method);

        var fooCalls = calls.stream()
                .filter(call -> call.getMethodNameTry().isPresent())
                .filter(call -> ((LiteralElement) call.getMethodName()).getLiteral().equals("\"foo\""))
                .toList();

        CpUtils.assertTrue("Expected one call to method 'foo' in method 'bar'", fooCalls.size() == 1, result);
        var fooCall = fooCalls.get(0);

        CpUtils.assertTrue("Expected two argument in call to 'foo'", fooCall.getArguments().size() == 2, result);
        CpUtils.assertTrue("Expected first argument in call to 'foo' to be an array", fooCall.getArguments().get(0).getType() instanceof ArrayType, result);
        CpUtils.assertTrue("Expected second argument in call to 'foo' to be an array", fooCall.getArguments().get(1).getType() instanceof ArrayType, result);

        var assigns = CpUtils.getInstructions(AssignInstruction.class, method);
        CpUtils.assertTrue("Expected at least 10 assignments in method 'bar'", assigns.size() >= 11, result);

    }

}
