package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class Operations extends AnalysisVisitor {




    @Override
    protected void buildVisitor() {
        addVisit(Kind.PARENTH_EXPR, this::visitParenthExpression);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpression);
        addVisit(Kind.NEG_EXPR, this::visitNegationExpression);
    }

    private Void visitBinaryExpression(JmmNode node, SymbolTable table) {
        String node_type = getExprType(node, table).getName();
        var left = node.getChild(0);
        var right = node.getChild(1);
        visit(left, table);
        visit(right, table);
        String message = "";
        if (node.get("op").equals("<")) {
            if (left.get("node_type").equals(right.get("node_type")) && left.get("node_type").equals("int")) {
                node.put("node_type", node_type);
                return null;
            } else {
                message = String.format(
                        "Binary Expression of type '%s' expected two %s operands, instead got types %s %s",
                        node.get("op"),
                        "int",
                        left.get("node_type"),
                        right.get("node_type")
                );
            }
        } else if (left.get("node_type").equals(right.get("node_type")) && left.get("node_type").equals(node_type)) {
            node.put("node_type", node_type);
            return null;
        } else {
            message = String.format(
                    "Binary Expression of type '%s' expected two %s operands, instead got types %s %s",
                    node.get("op"),
                    node_type,
                    left.get("node_type"),
                    right.get("node_type")
            );
        }
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null)
        );
        node.put("node_type", "null");
        return null;
    }



    private Void visitParenthExpression(JmmNode node, SymbolTable table) {
        JmmNode child = node.getChild(0);
        visit(child, table);
        node.put("node_type", child.get("node_type"));
        return null;
    }

    private Void visitNegationExpression(JmmNode node, SymbolTable table) {
        var expr = node.getChild(0);
        visit(expr, table);
        if (expr.get("node_type").equals("boolean")) {
            node.put("node_type", "boolean");
        } else {
            String message = String.format(
                    "Negation requires operand of type boolean, got %s instead.",
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















}
