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

public class CheckArrayInit extends AnalysisVisitor{
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_INITIALIZATION, this::visitArrayInitialization);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayInitialization(JmmNode arrayInitialization, SymbolTable symTable){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected method to be set");
        String arrayName = arrayInitialization.getJmmParent().getChild(0).get("name");
        String arrayType = "";
        for (var local: symTable.getLocalVariables(currentMethod)){
            if(local.getName().equals(arrayName)){
                System.out.println("cenas");
                if(!local.getType().isArray()){
                    var message = String.format("'%s' is not an Array", arrayName);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(arrayInitialization),
                            NodeUtils.getColumn(arrayInitialization),
                            message,
                            null)
                    );
                    return null;
                }else{
                    arrayType = local.getType().getName();
                }
            }
        }

        for(var value : arrayInitialization.getChildren()){
            System.out.println("batata");
            switch (arrayType){
                case "int":{
                    if(!value.getKind().equals("IntegerLiteral")){
                        var message = String.format("'%s' is not of type '%s", value.get("value"), arrayType);
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(arrayInitialization),
                                NodeUtils.getColumn(arrayInitialization),
                                message,
                                null)
                        );
                    }
                    break;
                }
                case "boolean":{
                    if(!value.getKind().equals("BooleanLiteral")){
                        var message = String.format("'%s' is not of type '%s", value.get("value"), arrayType);
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(arrayInitialization),
                                NodeUtils.getColumn(arrayInitialization),
                                message,
                                null)
                        );
                    }
                    break;
                }
            }
        }
        System.out.println("cenas");
        return null;
    }
}
