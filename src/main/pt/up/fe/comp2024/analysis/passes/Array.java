package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

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
        visit(variable, table);
        Matcher matcher = array_pattern.matcher(variable.get("node_type"));
        if (!(matcher.find() && matcher.group(2) != null)) {
            String message = String.format(
                    "Length attribute requires array, got %s instead",
                    variable.get("node_type")
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

        node.put("node_type", "int");
        return null;
    }

    private Void visitInitArrayExpression(JmmNode node, SymbolTable table) {
        visit(node.getChild(0), table);
        var type = node.getChild(0).get("node_type");
        for (var element : node.getChildren()) {
            visit(element, table);
            if (!element.get("node_type").equals(type)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Array can only be composed by elements of one type, multiple found.",
                        null)
                );
                return null;
            }
        }
        node.put("node_type", type + "_array");
        return null;
    }

    private Void visitArrayExpression(JmmNode node, SymbolTable table) {
        System.out.println("boas1");
        var left = node.getChild(0);
        var right = node.getChild(1);
        visit(left, table);
        visit(right, table);
        Matcher matcher = array_pattern.matcher(left.get("node_type"));
        System.out.println("boas2");

        if (!(matcher.find() && (matcher.group(2) != null || matcher.group(3) !=null))) {

            System.out.println("boas3");
            String message = String.format(
                    "Array expected, got %s instead",
                    left.get("node_type")
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
        if (!right.get("node_type").equals("int")) {
            System.out.println("boas4");
            String message = String.format(
                    "Array index must be of type int, got %s instead",
                    right.get("node_type")
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
        System.out.println("boas5");
        node.put("node_type", matcher.group(1));
        return null;
    }
}
