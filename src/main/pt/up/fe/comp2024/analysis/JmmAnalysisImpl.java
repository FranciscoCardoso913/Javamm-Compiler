package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp2024.ast_optimization.ASTOptimizationAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.passes.*;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

import java.util.List;

public class JmmAnalysisImpl implements JmmAnalysis {


    private final List<AnalysisPass> analysisPasses;

    public JmmAnalysisImpl() {

        this.analysisPasses = List.of(new UndeclaredVariable(),new NodeType(), new Duplicates(), new Operations(),new Init(), new Array(),  new Method(),new Statements());

    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        JmmNode rootNode = parserResult.getRootNode();

        JmmSymbolTableBuilder tableBuilder = new JmmSymbolTableBuilder();
        SymbolTable table = tableBuilder.build(rootNode);


        List<Report> reports = tableBuilder.getReports();

        for (Report report : reports) {
            if(report.getType() == ReportType.ERROR) new JmmSemanticsResult(parserResult, table, reports);
        }

        // Visit all nodes in the AST
        for (var analysisPass : analysisPasses) {
            try {
                var passReports = analysisPass.analyze(rootNode, table);

                reports.addAll(passReports);
                for (Report report : reports) {
                    if(report.getType() == ReportType.ERROR) return new JmmSemanticsResult(parserResult, table, reports);
                }
            } catch (Exception e) {
                reports.add(Report.newError(Stage.SEMANTIC,
                        -1,
                        -1,
                        "Problem while executing analysis pass '" + analysisPass.getClass() + "'",
                        e)
                );
            }

        }
        if(parserResult.getConfig().get("optimize") !=null && parserResult.getConfig().get("optimize").equals("true"))new ASTOptimizationAnalysis().optimize(parserResult.getRootNode());
        return new JmmSemanticsResult(parserResult, table, reports);
    }
}
