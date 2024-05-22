package ast_optimization;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;

public class ASTOptimizationVisitor extends PreorderJmmVisitor<SymbolTable, Void> {
    public Boolean opt = false;

    public ArrayList<Pair<String, String>> consts = new ArrayList<>();

    @Override
    protected void buildVisitor() {
        //addVisit(Kind.PROGRAM, this::visitProgram);
        addVisit(Kind.ASSIGN_STMT, this::constantPropagation);
        addVisit(Kind.BINARY_EXPR, this::constantFolding);
        addVisit(Kind.VAR_REF_EXPR, this::replaceVar);
        setDefaultVisit(this::defaultVisit);
    }


    public Void constantPropagation(JmmNode node, SymbolTable table) {
        var literals = new ArrayList<>();
        literals.add(Kind.BOOL_LITERAL.toString());
        literals.add(Kind.INTEGER_LITERAL.toString());
        var exp = node.getChild(0);
        visit(exp);
        if(literals.contains(exp.getKind())){
            consts.add(new Pair<>(node.get("name"), exp.get("value")));
        }else{
            consts.removeIf((pair) -> pair.a.equals(node.get("name")));
        }

        return null;
    }

    public Void constantFolding(JmmNode node, SymbolTable table) {
        JmmNode left = node.getChild(0);
        JmmNode right = node.getChild(1);
        var literals = new ArrayList<>();
        literals.add(Kind.BOOL_LITERAL.toString());
        literals.add(Kind.INTEGER_LITERAL.toString());
        if (literals.contains(left.getKind()) && literals.contains(right.getKind())) {
            node.replace(TypeUtils.calc(left.get("value"), right.get("value"), node.get("op")));
            this.opt = true;
        }
        return null;
    }

    private Void defaultVisit(JmmNode node, SymbolTable table) {
        // Ignore nodes that don't need specific handling
        visitAllChildren(node, table);
        return null;
    }
    public void optimize(JmmNode root, SymbolTable table){
        visit(root, table);
    }

    public Void replaceVar(JmmNode node, SymbolTable table ){
        boolean contain = false;
        for (var el : consts){
            System.out.println(el.a);
            if(el.a.equals(node.get("name"))){
                contain = true;
            }
            if(contain) {
                this.opt = true;
                System.out.println("fsjhdg");
                node.replace(NodeUtils.createLiteral(el.b));
                break;
            }
        }
        return null;
    }
}


