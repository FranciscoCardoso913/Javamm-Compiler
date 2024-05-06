package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

public class OptUtils {
    private static int tempNumber = -1;
    private static int ifNumber = -1;
    private static int whileNumber = -1;

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

    public static int getNextIfNum() {
        ifNumber++;
        return ifNumber;
    }

    public static int getNextWhileNum() {
        whileNumber++;
        return whileNumber;
    }

    public static String toOllirType(JmmNode node) {
        String[] type = node.get("node_type").split("_");
        return toOllirType(type[0], type.length > 1);
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
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
        return ollirVar.split("\\.")[0];
    }
}
