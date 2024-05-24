package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.regex.Matcher;

public class Array extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.INIT_ARRAY_EXPR, this::visitInitArrayExpression);
        addVisit(Kind.LENGTH_ATTR_EXPR, this::visitLengthAttributeExpression);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpression);
    }

    private Void visitLengthAttributeExpression(JmmNode node, SymbolTable table) {
        var variable = node.getChild(0);
        Matcher matcher = array_pattern.matcher(variable.get("node_type"));
        if (!(matcher.find() && (matcher.group(2) != null || matcher.group(3) != null ))) {
           addSemanticReport(node, String.format(
                   "Length attribute requires array, got %s instead",
                   variable.get("node_type")
           ));
        }
        return null;
    }

    private Void visitInitArrayExpression(JmmNode node, SymbolTable table) {
        if(node.getChildren().isEmpty()) return null;
        var type = node.getChild(0).get("node_type");
        for (var element : node.getChildren()) {
            visit(element, table);
            if (!element.get("node_type").equals(type)) {
                addSemanticReport(node, "Array can only be composed by elements of one type, multiple found.");
                return null;
            }
        }
        return null;
    }

    private Void visitArrayExpression(JmmNode node, SymbolTable table) {
        var right = node.getChild(1);
        if (!right.get("node_type").equals("int")) {
           addSemanticReport(node,  String.format(
                   "Array index must be of type int, got %s instead",
                   right.get("node_type")
           ));
        }
        return null;
    }
}
