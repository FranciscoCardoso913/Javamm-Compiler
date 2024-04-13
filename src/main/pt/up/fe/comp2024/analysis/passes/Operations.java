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
    private String currentMethod = "main";

    Pattern array_pattern = Pattern.compile("([a-zA-Z0-9]+)(_array)?");

    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStatement);
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
        addVisit(Kind.THIS, this::visitThis);
        addVisit(Kind.NEW_OBJ_EXPR, this::visitNewObjectExpression);
        addVisit(Kind.INIT_ARRAY_EXPR, this::visitInitArrayExpression);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpression);
        addVisit(Kind.METHOD, this::visitMethod);
        addVisit(Kind.LENGTH_ATTR_EXPR, this::visitLengthAttributeExpression);
        addVisit(Kind.PARENTH_EXPR, this::visitParenthExpression);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpression);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpression);
        addVisit(Kind.INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(Kind.BOOL_LITERAL, this::visitBooleanLiteral);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRef);
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

    private Void visitIntegerLiteral(JmmNode node, SymbolTable table) {
        node.put("node_type", "int");
        return null;
    }

    private Void visitBooleanLiteral(JmmNode node, SymbolTable table) {
        node.put("node_type", "boolean");
        return null;
    }

    private Void visitVarRef(JmmNode node, SymbolTable table) {
        String varRefName = node.get("name");
        node.put("node_type", NodeUtils.getLocalVariableType(varRefName, currentMethod, table));
        return null;
    }

    private Void visitParenthExpression(JmmNode node, SymbolTable table) {
        JmmNode child = node.getChild(0);
        visit(child, table);
        node.put("node_type", child.get("node_type"));
        return null;
    }

    private Void visitArrayExpression(JmmNode node, SymbolTable table) {
        var left = node.getChild(0);
        var right = node.getChild(1);
        visit(left, table);
        visit(right, table);
        Matcher matcher = array_pattern.matcher(left.get("node_type"));
        if (!(matcher.find() && matcher.group(2) != null)) {
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
        node.put("node_type", matcher.group(1));
        return null;
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

    private Void visitMethod(JmmNode node, SymbolTable table) {
        currentMethod = node.get("name");
        var return_statements = node.getChildren(Kind.RETURN_STMT);
        var method_type = node.getChild(0).get("name");
        String message = "";
        if (return_statements.isEmpty()) {
            if (method_type.equals("void")) {
                return null;
            } else {
                message = "Method of non type void should always return";
            }
        } else {
            var return_statement = return_statements.get(0);
            var return_value = return_statement.getChild(0);
            visit(return_value, table);
            Matcher matcher = array_pattern.matcher(return_value.get("node_type"));
            matcher.find();
            String type = matcher.group(1);

            boolean isArray = matcher.group(2) != null;
            if (type.equals(method_type) && Boolean.parseBoolean(node.getChild(0).get("isArray")) == isArray) {
                node.put("node_type", return_value.get("node_type"));
                return null;
            } else {
                message = String.format(
                        "Return value should be the same type as method type, got %s instead",
                        return_value.get("node_type")
                );
            }
        }
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null)
        );
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

    private Void visitThis(JmmNode node, SymbolTable table) {
        node.put("node_type", table.getClassName());
        return null;
    }

    private Void visitMethodExpr(JmmNode node, SymbolTable table) {
        //Check if method belongs to object
        var object = node.getChild(0);
        visit(object, table);
        System.out.println(table.getSuper());
        if(table.getImports().contains(object.get("node_type"))){
            node.put("node_type", "void");
            return null;
        }
        if (object.get("node_type").equals(table.getClassName())) {
            if(table.getSuper() != null){
                node.put("node_type", "void");
                return null;
            }
            if (table.getMethods().contains(node.get("name"))) {
                var method_params = table.getParameters(node.get("name"));
                if (method_params.size() != (node.getChildren().size() - 1)) {
                    String message = String.format(
                            "Expected to receive %s parameter, got %s instead.",
                            method_params.size(),
                            (node.getChildren().size() - 1)
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
                int i = 1;
                for (var method_param : method_params) {
                    String param_type = method_param.getType().getName() + (method_param.getType().isArray() ? "_array" : "");
                    var param = node.getChild(i);
                    visit(param, table);
                    if (!(param.get("node_type").equals(param_type))) {
                        String message = String.format(
                                "Expected parameter %s to be type %s, got %s instead.",
                                method_param.getName(),
                                param_type,
                                param.get("node_type")
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
                    i++;
                }
                var return_type = table.getReturnType(node.get("name"));
                node.put("node_type", return_type.getName() + (return_type.isArray() ? "_array" : ""));
                return null;
            }
            String message = String.format("%s does not contain method %s.", object.get("node_type"), node.get("name"));
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
        if( variable_type.equals(expr.get("node_type")))
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


}
