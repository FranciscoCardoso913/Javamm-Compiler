package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.NodeUtils.isImported;

public class Init extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.NEW_OBJ_EXPR, this::visitNewObjectExpression);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpression);
    }

    private Void visitNewObjectExpression(JmmNode node, SymbolTable table) {
        if (!node.get("name").equals(table.getClassName()) && !isImported(node.get("name"), table)) {
            addSemanticReport(node, node.get("name") + "class is undeclared.");
        }
        return null;
    }

    private Void visitNewArrayExpression(JmmNode node, SymbolTable table) {
        var size = node.getChild(0);
        if (!size.get("node_type").equals("int")) {
            addSemanticReport(node,  String.format(
                    "Array size must be of type int, got %s instead",
                    size.get("node_type")
            ));
        }
        return null;
    }
}
