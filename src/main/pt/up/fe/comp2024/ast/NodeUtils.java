package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

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
                // TODO: Extract this block to a different function?
                String isArray = variable.getType().isArray() ? "\narray" : "";
                String isEllipse = variable.getType().getObject("isEllipse", Boolean.class)?"\nellipse":"";
                return variable.getType().getName() + isArray + isEllipse;
            }
        }
        for (int i = 0; i < table.getParameters(currentMethod).size(); i++) {
            var variable = table.getParameters(currentMethod).get(i);
            if (variable.getName().equals(varRefName)) {
                String isArray = variable.getType().isArray() ? "\narray" : "";
                String isEllipse = variable.getType().getObject("isEllipse", Boolean.class)?"\nellipse":"";
                return variable.getType().getName() + isArray + isEllipse;
            }
        }

            for (int i = 0; i < table.getFields().size(); i++) {
                var variable = table.getFields().get(i);
                if (variable.getName().equals(varRefName)) {
                    if(currentMethod.equals("main")){
                        return null;
                    }
                    String isArray = variable.getType().isArray() ? "\narray" : "";
                    String isEllipse = variable.getType().getObject("isEllipse", Boolean.class)?"\nellipse":"";
                    return variable.getType().getName() + isArray + isEllipse;
                }
            }
        return "unknown";

    }

    // TODO: Maybe move this to symbolTable?
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

    // TODO: Maybe annotate tree so it isn't necessary to do this in Ollir
    public static boolean isFieldRef(String varRef, SymbolTable table, String currMethod) {
        for (Symbol local: table.getLocalVariables(currMethod)) {
            if (local.getName().equals(varRef))
                return false;
        }

        for (Symbol param: table.getParameters(currMethod)) {
            if (param.getName().equals(varRef))
                return false;
        }

        for (Symbol field: table.getFields()) {
            if (field.getName().equals(varRef))
                return true;
        }

        return false;
    }

    public static JmmNode createLiteral(String value){
        return switch (value){
            case "true", "false" -> createBooleanLiteral(value);
            default -> createIntegerLiteral(value);
        };
    }

    public static JmmNode createIntegerLiteral(String value){
        JmmNode node = new JmmNodeImpl("IntegerLiteral");
        node.put("node_type", "int");
        node.put("value", value);
        return node;
    }

    public static JmmNode createBooleanLiteral(String value){
        JmmNode node = new JmmNodeImpl("BoolLiteral");
        node.put("node_type", "boolean");
        node.put("value", value);
        return node;
    }
}
