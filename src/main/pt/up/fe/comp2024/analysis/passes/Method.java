package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.regex.Matcher;

public class Method extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
        addVisit(Kind.METHOD, this::visitMethod);
    }

    private Void visitMethodExpr(JmmNode node, SymbolTable table) {
        //Check if method belongs to object
        var object = node.getChild(0);
        visit(object, table);
        if(table.getImports().contains(object.get("node_type"))){
            node.put("node_type", "unknown");
            return null;
        }
        if (object.get("node_type").equals(table.getClassName())) {
            if(table.getSuper() != null){
                node.put("node_type", "void");
                return null;
            }
            if (table.getMethods().contains(node.get("name"))) {
                var method_params = table.getParameters(node.get("name"));
                /*if (method_params.size() != (node.getChildren().size() - 1)) {
                    System.out.println(method_params);
                    node.put("node_type", "undefined");
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
                }*/

                int method_param_idx= 0;
                for (int i =1; i< node.getChildren().size();i++) {
                    var method_param = method_params.get(method_param_idx);
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
                    if(!method_param.getType().getObject("isEllipse", Boolean.class)) method_param_idx++;
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
            if (return_value.get("node_type").equals("unknown")){
                node.put("node_type",method_type);
                return null;
            }
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
}
