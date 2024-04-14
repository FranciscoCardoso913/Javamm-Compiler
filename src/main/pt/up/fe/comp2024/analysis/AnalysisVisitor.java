package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
public abstract class AnalysisVisitor extends PreorderJmmVisitor<SymbolTable, Void> implements AnalysisPass {

    private List<Report> reports;
    protected static String currentMethod = "main";

    protected Pattern array_pattern = Pattern.compile("([a-zA-Z0-9]+)(_array)?(_ellipse)?");

    public AnalysisVisitor() {
        reports = new ArrayList<>();
        setDefaultValue(() -> null);
    }

    protected void addReport(Report report) {
        reports.add(report);
    }

    protected List<Report> getReports() {
        return reports;
    }


    @Override
    public List<Report> analyze(JmmNode root, SymbolTable table) {
        // Visit the node
        visit(root, table);

        // Return reports
        return getReports();
    }
}
