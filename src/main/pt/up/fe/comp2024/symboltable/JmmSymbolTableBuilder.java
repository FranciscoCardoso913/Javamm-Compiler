package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

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

    private List<Report> reports;

    public JmmSymbolTableBuilder() {
        imports = new ArrayList<>();
        fields = new ArrayList<>();
        methods = new ArrayList<>();
        returnTypes = new HashMap<>();
        params = new HashMap<>();
        locals = new HashMap<>();
        reports = new ArrayList<>();
    }

    public List<Report> getReports(){
        return reports;
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
        addVisit("Method", this::dealWithMethod);
        addVisit("LengthAttrExpr", this::dealWithLength);
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
        superName = jmmNode.getOptional("extendedClass").orElse(null);

        for (JmmNode child : jmmNode.getChildren())
            visit(child);

        return null;
    }

    private Void dealWithVarDecl(JmmNode jmmNode, Void v) {
        fields.add(new Symbol(getType(jmmNode.getChild(0)), jmmNode.get("name")));
        return null;
    }

    private Type getType(JmmNode typeNode) {
        boolean isArray = Boolean.parseBoolean(typeNode.get("isArray")) ;
        boolean isEllipse = Boolean.parseBoolean(typeNode.get("isEllipse"));
        Type type= new Type(typeNode.get("name"), isArray);
        type.putObject("isEllipse", isEllipse);
        return type;
    }

    private Void dealWithMethod(JmmNode jmmNode, Void v) {
        List<Symbol> methodParams = new ArrayList<>();
        List<Symbol> methodLocals = new ArrayList<>();
        String methodName = jmmNode.get("name");
        // TODO: change to boolean
        if(jmmNode.get("isMain").equals("true")){
            String error_message = "";
            if(!methodName.equals("main"))
                error_message = "Expected 'main' as method's name, got '" + jmmNode.get("name") + "' instead";
            else if(jmmNode.getChildren(Kind.PARAM).size() != 1)
                error_message = "Expected 1 param for mainMethod, got '" + jmmNode.getChildren(Kind.PARAM).size() + "' instead";
            else if (!jmmNode.getChildren(Kind.PARAM).get(0).getChild(0).get("name").equals("String"))
                error_message = "Expected mainMethod param should be of type 'String', got '" + jmmNode.getChildren(Kind.PARAM).get(0).getChild(0).get("name") + "' instead";
            else if (!jmmNode.getChildren(Kind.PARAM).get(0).getChild(0).get("isArray").equals("true"))
                error_message = "Expected mainMethod param to be an array";


            if(!error_message.isEmpty()){
                this.reports.add( Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(jmmNode),
                                NodeUtils.getColumn(jmmNode),
                                error_message,
                                null
                        )
                );
                return null;
            }

        }

        methods.add(methodName);
        returnTypes.put(methodName, getType(jmmNode.getChild(0)));
        for (JmmNode param : jmmNode.getChildren(Kind.PARAM)) {
            methodParams.add(new Symbol(getType(param.getChild(0)), param.get("name")));
        }
        params.put(methodName, methodParams);

        for (JmmNode varDecl : jmmNode.getChildren(Kind.VAR_DECL)) {
            methodLocals.add(new Symbol(getType(varDecl.getChild(0)), varDecl.get("name")));
        }
        locals.put(methodName, methodLocals);

        for (JmmNode expr : jmmNode.getChildren(Kind.LENGTH_ATTR_EXPR)) {
            visit(expr);
        }

        return null;
    }

    private Void dealWithLength(JmmNode jmmNode, Void v) {
        if ( !jmmNode.get("name").equals("length")){
            this.reports.add( Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    ("Expected Length attribute, got '" + jmmNode.get("name") + "' instead"),
                    null
                )
            );
        }
        return null;
    }



}


