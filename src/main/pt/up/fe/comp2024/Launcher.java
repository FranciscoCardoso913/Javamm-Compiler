package pt.up.fe.comp2024;

import ast_optimization.ASTOptimizationAnalysis;
import ast_optimization.ASTOptimizationVisitor;
import org.specs.comp.ollir.OllirErrorException;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2024.analysis.JmmAnalysisImpl;
import pt.up.fe.comp2024.backend.JasminBackendImpl;
import pt.up.fe.comp2024.optimization.JmmOptimizationImpl;
import pt.up.fe.comp2024.parser.JmmParserImpl;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

import java.util.Arrays;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) throws NoSuchFieldException, OllirErrorException {
        SpecsSystem.programStandardInit();

        Map<String, String> config = CompilerConfig.parseArgs(args);

        var inputFile = CompilerConfig.getInputFile(config).orElseThrow();
        if (!inputFile.isFile()) {
            throw new RuntimeException("Option '-i' expects a path to an existing input file, got '" + args[0] + "'.");
        }
        String code = SpecsIo.read(inputFile);

        // Parsing stage
        JmmParserImpl parser = new JmmParserImpl();
        JmmParserResult parserResult = parser.parse(code, config);
        TestUtils.noErrors(parserResult.getReports());
        // Print AST
        //System.out.println(parserResult.getRootNode().toTree());


        // Semantic Analysis stage
        JmmAnalysisImpl sema = new JmmAnalysisImpl();
        JmmSemanticsResult semanticsResult = sema.semanticAnalysis(parserResult);
        //parserResult.getRootNode().add(new JmmNodeImpl("IntegerLiteral"));
        System.out.println(parserResult.getRootNode().toTree());
        TestUtils.noErrors(semanticsResult.getReports());
        System.out.println("ola");


        //System.out.println(semanticsResult.getSymbolTable().getLocalVariables("main"));
        System.out.println(parserResult.getRootNode().toTree());

        // Optimization stage
        JmmOptimizationImpl ollirGen = new JmmOptimizationImpl();
        OllirResult ollirResult = ollirGen.toOllir(semanticsResult);
        TestUtils.noErrors(ollirResult.getReports());
        System.out.println("Ollir");
        ollirResult.getOllirClass().buildCFGs();
        ollirGen.optimize(ollirResult);
        System.out.println("Ollir");
        System.out.println(ollirResult.getOllirClass().getMethods().get(0).getVarTable().keySet());
        var shit = (ollirResult.getOllirClass().getMethods().get(0).getInstructions().get(0).getChildren().get(0).toString().split(":")[1].split("($[1-9]+)?\\.")[0].strip());
        /*for (var yoMama: shit){
            System.out.println(yoMama);
        }*/
        System.out.println(shit);


        // Print OLLIR code
        System.out.println(ollirResult.getOllirCode());

        // Code generation stage
        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin(ollirResult);
        TestUtils.noErrors(jasminResult.getReports());

        // Print Jasmin code
        //System.out.println(jasminResult.getJasminCode());
    }

}
