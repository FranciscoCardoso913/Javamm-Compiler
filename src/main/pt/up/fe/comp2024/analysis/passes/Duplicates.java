package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashSet;
import java.util.Set;

public class Duplicates extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassFields);
        addVisit(Kind.METHOD, this::visitMethods);
        addVisit(Kind.PROGRAM, this::visitImport);
    }
    private Void visitClassFields(JmmNode node, SymbolTable table){
        Set<String> set = new HashSet<>();
        for (var field: node.getChildren(Kind.VAR_DECL)) {
            if(set.contains(field.get("name"))) addSemanticReport(node, String.format(
                    "Duplicated field %s",
                    field.get("name")
            ));
            else set.add(field.get("name"));
        }
        set = new HashSet<>();
        for (var method: node.getChildren(Kind.METHOD)) {
            if(set.contains(method.get("name"))) addSemanticReport(node, String.format(
                    "Duplicated method %s",
                    method.get("name")
            ));
            else set.add(method.get("name"));
        }
        return null;
    }
    private Void visitMethods(JmmNode node, SymbolTable table){
        Set<String> set = new HashSet<>();
        for (var param: node.getChildren(Kind.PARAM)) {
            if(set.contains(param.get("name"))) addSemanticReport(node, String.format(
                    "Duplicated param %s",
                    param.get("name")
            ));
            else set.add(param.get("name"));
        }
        set = new HashSet<>();
        for (var varRef: node.getChildren(Kind.VAR_DECL)) {
            if(set.contains(varRef.get("name"))) addSemanticReport(node, String.format(
                    "Duplicated variable %s",
                    varRef.get("name")
            ));
            else set.add(varRef.get("name"));
        }
        return null;
    }
    private Void visitImport(JmmNode node, SymbolTable table){
        Set<String> set = new HashSet<>();
        for (var imported_path: node.getChildren(Kind.IMPORT_DECL)) {
            String[] parts = imported_path.get("path").replaceAll("[\\[\\]\\s]", "").split(",");
            String className = parts[parts.length - 1];
            if(set.contains(className)) addSemanticReport(node, String.format(
                    "Duplicated import %s",
                    className
            ));
            else set.add(className);
        }

        return null;
    }
}
