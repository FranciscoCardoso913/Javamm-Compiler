package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.ArrayList;

import static pt.up.fe.comp2024.ast.TypeUtils.*;

public class Method extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
        addVisit(Kind.METHOD, this::visitMethod);

    }

    private Void visitMethodExpr(JmmNode node, SymbolTable table) {
        if (node.get("node_type").equals("unknown") || node.get("node_type").equals("undefined")) return null;
        var method_params = table.getParameters(node.get("name"));
        int method_param_idx = 0;
        int invoc_param_idx = 1;
        boolean isEll = false;
        int method_params_size = method_params.size();
        int invoc_params_size = node.getChildren().size();
        while (method_param_idx<method_params.size() && invoc_param_idx< node.getChildren().size()){
             isEll = method_params.get(method_param_idx).getType().getObject("isEllipse", Boolean.class);
             if (isEll && method_param_idx != method_params.size() -1){
                 addSemanticReport(node, "Ellipses should be in the last parameter");
                 return null;
             }
             if (isEll && isArray(node.getChild(invoc_param_idx).get("node_type"))) {
                 invoc_params_size --;
                 break;
             }
             if(! getType(method_params.get(method_param_idx).getType()).equals( node.getChild(invoc_param_idx).get("node_type"))){
                addSemanticReport(node, String.format(
                        "Expected parameter %s to be type %s, got %s instead.",
                        method_params.get(method_param_idx).getName(),
                        method_params.get(method_param_idx).getType().getName(),
                        node.getChild(invoc_param_idx).get("node_type")
                ));
                return null;
            }
            if(!isEll) method_param_idx++;
            if(!isEllipse(node.getChild(invoc_param_idx).get("node_type"))) invoc_param_idx++;
        };



        if( isEll ) method_params_size--;

        if (!((method_param_idx==method_params_size && invoc_param_idx==invoc_params_size) || (method_param_idx + 1 == method_params_size && method_params.get(method_params.size() - 1).getType().getObject("isEllipse", Boolean.class) && invoc_param_idx==invoc_params_size))){
            addSemanticReport(node, String.format(
                    "Expected %s parameters, got %s instead.",
                    method_params.size(),
                    node.getChildren().size()-1

            ));
        }
        return null;
    }

    private Void visitMethod(JmmNode node, SymbolTable table){
        int idx =1 ;
        for (var param : node.getChildren(Kind.PARAM)){
            var type = param.getChild(0);
            if (Boolean.parseBoolean(type.get("isEllipse")) && idx != node.getChildren(Kind.PARAM).size() )
                addSemanticReport(node, "Ellipse should be the last parameter");
            idx ++;
        }
        var variables = new ArrayList<String>();
        var params = new ArrayList<String>();
        for (var varRef: node.getChildren(Kind.VAR_DECL)){
            variables.add(varRef.get("name"));
        }
        for (var param: node.getChildren(Kind.PARAM)){
            params.add(param.get("name"));
        }
        for (var variable: variables)
            if( params.contains(variable))
                addSemanticReport(node, "Redeclaration");
        return null;
    }


}
