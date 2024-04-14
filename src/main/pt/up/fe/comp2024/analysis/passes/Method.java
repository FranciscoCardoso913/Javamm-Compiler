package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.regex.Matcher;


import static pt.up.fe.comp2024.ast.TypeUtils.isEllipse;

public class Method extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);

    }

    private Void visitMethodExpr(JmmNode node, SymbolTable table) {
        if (node.get("node_type").equals("unknown") || node.get("node_type").equals("undefined")) return null;
        var method_params = table.getParameters(node.get("name"));
        int method_param_idx = 0;
        int invoc_param_idx = 1;
        boolean isEll = false;
        while (method_param_idx<method_params.size() && invoc_param_idx< node.getChildren().size()){
             isEll = method_params.get(method_param_idx).getType().getObject("isEllipse", Boolean.class);
             if (isEll && method_param_idx != method_params.size() -1){
                 addSemanticReport(node, "Ellipses should be in the last parameter");
                 return null;
             }
             if(!method_params.get(method_param_idx).getType().getName().equals( node.getChild(invoc_param_idx).get("node_type"))){
                addSemanticReport(node, String.format(
                        "Expected parameter %s to be type %s, got %s instead.",
                        method_params.get(method_param_idx).getName(),
                        method_params.get(method_param_idx).getType().getName(),
                        node.getChild(invoc_param_idx).get("node_type")
                ));
                return null;
            }
            if(!isEll) method_param_idx++;
            if(!isEllipse(node.getChild(invoc_param_idx).get("node_type"))) invoc_param_idx ++;
        };

        int method_params_size = method_params.size();

        if( isEll ) method_params_size --;

        if (!(method_param_idx==method_params_size && invoc_param_idx==node.getChildren().size())){
            addSemanticReport(node, String.format(
                    "Expected %s parameters, got %s instead.",
                    method_params.size(),
                    node.getChildren().size()-1

            ));
        }
        return null;
    }


}
