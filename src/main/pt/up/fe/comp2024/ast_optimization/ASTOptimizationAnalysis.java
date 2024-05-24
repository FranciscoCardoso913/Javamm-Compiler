package pt.up.fe.comp2024.ast_optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

public class ASTOptimizationAnalysis {
    private ASTOptimizationVisitor optimizationVisitor;
    public ASTOptimizationAnalysis(){
        this.optimizationVisitor = new ASTOptimizationVisitor();
        this.optimizationVisitor.buildVisitor();
    }

    public void optimize(JmmNode rootNode){
        JmmSymbolTableBuilder tableBuilder = new JmmSymbolTableBuilder();
        SymbolTable table = tableBuilder.build(rootNode);
        do {
            this.optimizationVisitor.opt= false;
            this.optimizationVisitor.consts.clear();
            this.optimizationVisitor.optimize(rootNode, table);
        }while (this.optimizationVisitor.opt);
    }
}
