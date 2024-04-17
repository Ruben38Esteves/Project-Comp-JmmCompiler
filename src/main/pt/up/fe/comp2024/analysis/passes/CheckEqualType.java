package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;
public class CheckEqualType extends AnalysisVisitor{
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable symTable){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected method to be set");
        JmmNode left = assignStmt.getChild(0);
        JmmNode right = assignStmt.getChild(1);
        List<Symbol> symList = symTable.getLocalVariables(currentMethod);
        List<String> imports = symTable.getImports();
        String rightType = "";
        String leftType = "";
        boolean isLeftArray = false;
        boolean isRightArray = false;

        for (var sym: symList){
            if (sym.getName().equals(left.get("name"))) {
                leftType = sym.getType().getName();
                isLeftArray = sym.getType().isArray();
            }
        }

        if(right.getKind().equals("NewArray")){
            rightType = right.getChild(0).get("name");
            right = right.getChild(1);
        }

        if(right.getKind().equals("ClassInstance")){
            rightType = right.get("name");
        }

        //adaptar para chain de methods
        if(right.getKind().equals("MethodExpr")){
            String var_name = right.getChild(0).get("name");
            Type return_type = symTable.getReturnType(right.getChild(1).get("name"));
            if(return_type != null){
                rightType = return_type.getName();
            }else{
                for(var sym : symList){
                    if(sym.getName().equals(var_name) && imports.contains(sym.getType().getName())){
                        return null;
                    }
                    if(sym.getName().equals(var_name) && symTable.getSuper() != null ){
                        if(sym.getType().getName().equals(symTable.getSuper()) && imports.contains(symTable.getSuper())){
                            return null;
                        }
                    }
                }
                var message = String.format("Class %s not imported", var_name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }

        if(right.getKind().equals("IntegerLiteral")){
            if(leftType.equals("int")){
                return null;
            }else {
                var message = String.format("Cannot attribute int to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }

        if(right.getKind().equals("BooleanLiteral")){
            if(leftType.equals("boolean")){
                return null;
            }else{
                var message = String.format("Cannot attribute boolean to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }

        if(right.getKind().equals("ArrayInitialization")){
            if(isLeftArray){
                return null;
            }else {
                var message = String.format("Cannot attribute array to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }


        if(rightType.isEmpty()){
            for (var sym: symList){
                if (sym.getName().equals(right.get("name"))) {
                    rightType = sym.getType().getName();
                    isRightArray = sym.getType().isArray();
                }
            }
        }


        if(!leftType.equals(rightType)){
            if(imports.contains(leftType)){
                if (imports.contains(rightType)){
                    return null;
                }
                if(leftType.equals(symTable.getSuper()) && rightType.equals(symTable.getClassName())){
                    return null;
                }
            }
            var message = String.format("Cannot attribute '%s' to '%s'", rightType, leftType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null));
            return null;
        } else if (isLeftArray != isRightArray) {
            var message = String.format("Cannot attribute '%s' to '%s' beacause of them is not an array", rightType, leftType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null));
            return null;

        }


        return null;
    }

}
