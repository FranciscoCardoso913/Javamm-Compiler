package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

public class OptUtils {
    private static int tempNumber = -1;
    private static int ifNumber = -1;
    private static int whileNumber = -1;
    private static int andNumber = -1;

    private final static String VIRTUAL_FUNC = "invokevirtual";
    private final static String STATIC_FUNC = "invokestatic";

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static void decrementTempNum() {
        tempNumber -= 1;
    }

    public static int getNextIfNum() {
        ifNumber++;
        return ifNumber;
    }

    public static int getNextWhileNum() {
        whileNumber++;
        return whileNumber;
    }

    public static int getNextAndNumber() {
        andNumber++;
        return andNumber;
    }

    public static String toOllirType(JmmNode node) {
        String type = node.get("node_type");
        boolean isArray = TypeUtils.isArray(type) || TypeUtils.isEllipse(type);
        List<String> separatedType = List.of(node.get("node_type").split("\n"));
        int lastIdx = isArray ? separatedType.size() - 1 : separatedType.size();
        return toOllirType(String.join("_", separatedType.subList(0, lastIdx)), isArray);
    }

    public static String toOllirType(JmmNode node, boolean consider_array) {
        String type = node.get("node_type");
        boolean isArray = TypeUtils.isArray(type) || TypeUtils.isEllipse(type);
        List<String> separatedType = List.of(node.get("node_type").split("\n"));
        int lastIdx = isArray && consider_array ? separatedType.size() - 2 : separatedType.size() - 1;
        return toOllirType(String.join("_", separatedType.subList(0, lastIdx)), isArray && consider_array);
    }

    public static String toOllirType(Type type) {
        boolean isEllispe = false;

        if (type.getAttributes().contains("isEllipse"))
            isEllispe = type.getObject("isEllipse", Boolean.class);

        return toOllirType(type.getName(), type.isArray() || isEllispe);
    }

    private static String toOllirType(String typeName, boolean isArray) {
        String type = (isArray ? ".array" : "");

        type = type + "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "void" -> "V";
            default -> typeName;
        };

        return type;
    }

    public static String getOllirMethod(SymbolTable table, String objName) {
        StringBuilder code = new StringBuilder();
        boolean isStatic = NodeUtils.isImported(objName, table) || objName.equals(table.getClassName());
        String funcType = isStatic ? STATIC_FUNC : VIRTUAL_FUNC;

        code.append(funcType).append("(").append(objName);

        return code.toString();
    }

    public static String removeOllirType(String ollirVar) {
        String[] parts = ollirVar.split("\\.");

        if (parts.length < 3 || (parts.length == 3 && isOllirArray(ollirVar)))
            return parts[0];
        else
            return parts[0] + "." + parts[1];
    }

    public static boolean isOllirArray(String ollirVar) {
        String[] parts = ollirVar.split("\\.");

        return parts[parts.length - 2].equals("array");
    }

    public static String getOllirBaseType(String ollirVar) {
        String[] parts = ollirVar.split("\\.");

        return parts[parts.length - 1];
    }
}
