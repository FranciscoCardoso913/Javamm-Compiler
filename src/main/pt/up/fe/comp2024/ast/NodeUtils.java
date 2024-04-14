package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class NodeUtils {

    public static int getLine(JmmNode node) {

        return getIntegerAttribute(node, "lineStart", "-1");
    }

    public static int getColumn(JmmNode node) {

        return getIntegerAttribute(node, "colStart", "-1");
    }

    public static int getIntegerAttribute(JmmNode node, String attribute, String defaultVal) {
        String line = node.getOptional(attribute).orElse(defaultVal);
        return Integer.parseInt(line);
    }

    public static boolean getBooleanAttribute(JmmNode node, String attribute, String defaultVal) {
        String line = node.getOptional(attribute).orElse(defaultVal);
        return Boolean.parseBoolean(line);
    }
    public static String getLocalVariableType(String varRefName, String currentMethod, SymbolTable table){
        for (int i = 0; i < table.getLocalVariables(currentMethod).size(); i++) {
            var variable = table.getLocalVariables(currentMethod).get(i);
            if (variable.getName().equals(varRefName)) {
                String isArray = variable.getType().isArray() ? "_array" : "";
                String isEllipse = variable.getType().getObject("isEllipse", Boolean.class)?"_ellipse":"";
                return variable.getType().getName() + isArray + isEllipse;
            }
        }
        for (int i = 0; i < table.getParameters(currentMethod).size(); i++) {
            var variable = table.getParameters(currentMethod).get(i);
            if (variable.getName().equals(varRefName)) {
                String isArray = variable.getType().isArray() ? "_array" : "";
                String isEllipse = variable.getType().getObject("isEllipse", Boolean.class)?"_ellipse":"";
                return variable.getType().getName() + isArray + isEllipse;
            }
        }
        for (int i = 0; i < table.getFields().size(); i++) {
            var variable = table.getFields().get(i);
            if (variable.getName().equals(varRefName)) {
                String isArray = variable.getType().isArray() ? "_array" : "";
                String isEllipse = variable.getType().getObject("isEllipse", Boolean.class)?"_ellipse":"";
                return variable.getType().getName() + isArray + isEllipse;
            }
        }
        return "unknown";

    }

    public static boolean isImported(String name, SymbolTable table){
        for (var imported_path : table.getImports()) {
            String[] parts = imported_path.split("\\.");
            String className = parts[parts.length - 1];
            if (className.equals(name)) {
                return true;
            }
        }
        return false;
    }


}
