package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class Init extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.NEW_OBJ_EXPR, this::visitNewObjectExpression);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpression);
    }

    private Void visitNewObjectExpression(JmmNode node, SymbolTable table) {
        // Verify if the class exists
        boolean isImported = false;
        for (var imported_path : table.getImports()) {
            String[] parts = imported_path.split("\\.");
            String className = parts[parts.length - 1];
            if (className.equals(node.get("name"))) {
                isImported = true;
                break;
            }
        }
        if (node.get("name").equals(table.getClassName()) || isImported) {
            node.put("node_type", node.get("name"));
            return null;
        }

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                node.get("name") + "class is undeclared.",
                null)
        );

        return null;
    }

    private Void visitNewArrayExpression(JmmNode node, SymbolTable table) {
        var size = node.getChild(0);
        visit(size, table);
        if (size.get("node_type").equals("int")) {
            node.put("node_type", "int_array");
            return null;
        }
        String message = String.format(
                "Array size must be of type int, got %s instead",
                size.get("node_type")
        );
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null)
        );
        return null;
    }
}
