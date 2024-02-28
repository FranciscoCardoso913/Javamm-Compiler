package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;

public class JmmSymbolTableBuilder extends AJmmVisitor<Void, Void> {
    private List<String> imports;
    private String className;
    private String superName;
    private List<Symbol> fields;
    private List<String> methods;
    private Map<String, Type> returnTypes;
    private Map<String, List<Symbol>> params;
    private Map<String, List<Symbol>> locals;

    public JmmSymbolTableBuilder() {
        imports = new ArrayList<>();
        fields = new ArrayList<>();
        methods = new ArrayList<>();
        returnTypes = new HashMap<>();
        params = new HashMap<>();
        locals = new HashMap<>();
    }

    public JmmSymbolTable build(JmmNode root) {
        visit(root, null);
        return new JmmSymbolTable(
                imports,
                className,
                superName,
                fields,
                methods,
                returnTypes,
                params,
                locals
        );
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ImportDecl", this::dealWithImport);
        addVisit("ClassDecl", this::dealWithClass);
        addVisit("VarDecl", this::dealWithVarDecl);
    }

    private Void dealWithProgram(JmmNode jmmNode, Void v) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child);

        return null;
    }

    private Void dealWithImport(JmmNode jmmNode, Void v) {
        List<String> sub_paths = jmmNode.getObjectAsList("path", String.class);
        imports.add(String.join(".", sub_paths));
        return null;
    }

    private Void dealWithClass(JmmNode jmmNode, Void v) {
        className = jmmNode.get("name");
        superName = jmmNode.getOptional("extendedClass").orElse("");

        for (JmmNode child : jmmNode.getChildren())
            visit(child);

        return null;
    }

    private Void dealWithVarDecl(JmmNode jmmNode, Void v) {
        fields.add(new Symbol(getType(jmmNode.getJmmChild(0)), jmmNode.get("name")));
        return null;
    }

    private Type getType(JmmNode typeNode) {
        // TODO: Verify if node is actually a type or use visit? To use visit we have to change the return type
        // of the node which may be strange as it will only be used here...
        // System.out.println(typeNode.getKind());

        // TODO: Its probably better to not distinguish between '...' and '[]' in node annotation
        boolean isArray = Boolean.parseBoolean(typeNode.get("isArray")) || Boolean.parseBoolean(typeNode.get("isEllipse"));

        return new Type(typeNode.get("name"), isArray);
    }

    /*

    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getJmmChild(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getIntTypeName(), false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), Arrays.asList(new Symbol(intType, method.getJmmChild(1).get("name")))));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

     */
}
