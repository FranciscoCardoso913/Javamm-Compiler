package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    private String extractLhs(TreeNode node){
        return node.toString().split(":")[1].split("($[1-9]+)?\\.")[0].strip();
    }
    private HashSet<String> def(Instruction n){
        HashSet<String > res = new HashSet<>();
        if(n.getInstType().toString().equals("ASSIGN")){
            res.add(extractLhs(n.getChildren().get(0)));
        }
        return res;
    }
    private boolean isOperand(TreeNode n){
        return n.toString().split(":")[0].equals("Operand");
    }

    private HashSet<String> visit(TreeNode n){
        HashSet<String> res = new HashSet<>();
        if(isOperand(n)){
            res.add(extractLhs(n));
        }else{
            for (var child: n.getChildren()){
                res.addAll(visit(child));
            }
        }
        return res;
    }

    private HashSet<String> use(Instruction n){
        HashSet<String > res = new HashSet<>();
        if(n.getInstType().toString().equals("ASSIGN")) {
            res.addAll(visit(n.getChildren().get(1)));
        }
        else {
            for (var child : n.getChildren())
                res.addAll(visit(child));
        }
        return  res;
    }

    private HashSet<String> in (Instruction n, HashSet<String> out){
        HashSet<String> copy = new HashSet<>(out);
        copy.removeAll(def(n));
        HashSet<String > res = use(n);
        res.addAll(copy);
        return  res;
    }

    private HashSet<String> out(Instruction n, ArrayList<HashSet<String>> in, int i){
        int j= i+1;
        HashSet<String> s = new HashSet<>();
        if(in.size() == i+1) return s;
        for (var suc : n.getSuccessors()){
            s.addAll(in.get(j));
            j++;
        }
        return  s;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {


        if(ollirResult.getConfig().get("registerAllocation")!=null && !ollirResult.getConfig().get("registerAllocation").equals("-1")) {
            int maxSize = Integer.parseInt(ollirResult.getConfig().get("registerAllocation"));
            int maxUsed=0;
            for (var method : ollirResult.getOllirClass().getMethods()) {
                var inArray = new ArrayList<HashSet<String>>();
                var outArray = new ArrayList<HashSet<String>>();
                var inLine = new ArrayList<HashSet<String>>();
                var outLine = new ArrayList<HashSet<String>>();
                for (var i = 0; i < method.getInstructions().size(); i++) {
                    inArray.add(new HashSet<>());
                    outArray.add(new HashSet<>());
                    inLine.add(new HashSet<>());
                    outLine.add(new HashSet<>());
                }
                boolean res = false;
                inArray.set(0, new HashSet<>());
                do {

                    for (var i = 0; i < method.getInstructions().size(); i++) {
                        inLine.set(i, inArray.get(i));
                        outLine.set(i, outArray.get(i));
                        inArray.set(i, in(method.getInstructions().get(i), outLine.get(i)));
                        outArray.set(i, out(method.getInstructions().get(i), inArray, i));
                    }
                    //System.out.println("ola" + inArray);
                    //System.out.println("ola2" + outArray);
                    res = true;
                    for (var i = 0; i < method.getInstructions().size(); i++) {
                        res = res && (inLine.get(i).equals(inArray.get(i))) && (outLine.get(i).equals(outArray.get(i)));
                    }

                } while (!res);
                var keySet = method.getVarTable().keySet();
                var map = new HashMap<String, HashSet<String>>();
                var intMap = new HashMap<String, Integer>();
                for (var key : keySet) {
                    intMap.put(key, 0);
                    map.put(key, new HashSet<>());
                    for (var inst : inArray) {
                        if (inst.contains(key)) map.get(key).addAll(inst);
                    }
                    for (var inst : outArray) {
                        if (inst.contains(key)) map.get(key).addAll(inst);
                    }

                }

                for (var key : keySet) {

                    boolean valid = false;
                    while (!valid) {
                        valid = true;
                        for (var el : map.get(key)) {
                            if (!el.equals(key)) {
                                if (intMap.get(el).equals(intMap.get(key))) {
                                    maxUsed = Math.max(maxUsed, intMap.get(key) + 1);
                                    intMap.put(key, intMap.get(key) + 1);
                                    valid = false;
                                }
                            }
                        }
                    }
                }

                if( maxUsed >= maxSize){
                    ollirResult.getReports().add(Report.newError(Stage.OPTIMIZATION, -1,-1,String.valueOf(maxUsed+1),new RuntimeException()));
                }

                for (var key : keySet) {
                    method.getVarTable().get(key).setVirtualReg(intMap.get(key));
                }

            }
        }
        return ollirResult;
    }
}
