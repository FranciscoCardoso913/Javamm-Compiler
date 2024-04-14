package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class Statements extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.WHILE_STMT, this::visitWhileStatement);
        addVisit(Kind.IF_STMT, this::visitIfStatement);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStatement);
    }

    private Void visitWhileStatement(JmmNode node, SymbolTable table) {
        visit(node.getChild(0), table);
        if(!node.getChild(0).get("node_type").equals("boolean")){
            String message = String.format(
                    "While statement should receive type boolean, got %s instead",
                    node.getChild(0).get("node_type")
            );
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }


        return null;
    }

    private Void visitAssignStatement(JmmNode node, SymbolTable table) {
        var variable_type = NodeUtils.getLocalVariableType(node.get("name"), currentMethod, table);
        var expr = node.getChild(0);
        visit( expr, table);
        // TODO ors instead of if-else
        if(expr.get("node_type").equals("unknown") ){
            node.put("node_type", variable_type);
        }
        else if( variable_type.equals(expr.get("node_type")))
            node.put("node_type", variable_type);
        else if(expr.get("node_type").equals(table.getClassName()) && variable_type.equals(table.getSuper())){
            node.put("node_type", variable_type);
        }
        else if (table.getImports().contains(variable_type) && table.getImports().contains(expr.get("node_type"))){
            node.put("node_type", variable_type);
        }
        else {
            String message = String.format(
                    "Variable of type %s cannot be assign a value of type %s.",
                    variable_type,
                    expr.get("node_type")
            );
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitIfStatement(JmmNode node, SymbolTable table) {
        visit(node.getChild(0), table);
        if(!node.getChild(0).get("node_type").equals("boolean")){
            String message = String.format(
                    "If statement should receive type boolean, got %s instead",
                    node.getChild(0).get("node_type")
            );
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }


        return null;
    }

}
