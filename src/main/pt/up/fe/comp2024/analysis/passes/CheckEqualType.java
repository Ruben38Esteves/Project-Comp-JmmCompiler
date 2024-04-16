package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
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
        addVisit(Kind.ASSIGN_STMT, this::visitStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
    }

    private Void visitStmt(JmmNode assignStmt, SymbolTable symTable){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected method to be set");
        JmmNode left = assignStmt.getChild(0);
        JmmNode right = assignStmt.getChild(1);
        List<Symbol> symList = symTable.getLocalVariables(currentMethod);
        String rightType = "";
        String leftType = "";

        for (var sym: symList){
            if (sym.getName().equals(left.get("name"))) {
                leftType = sym.getType().getName();
            }
        }

        if (leftType.equals("boolean")){
            if (right.getKind().equals("IntegerLiteral")){
                var message = String.format("Cannot perform int to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
            }
        }


        return null;
    }

}
