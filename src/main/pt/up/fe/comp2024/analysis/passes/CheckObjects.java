package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;

public class CheckObjects extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_INSTANCE, this::visitClassInstance);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitClassInstance(JmmNode class_instance, SymbolTable symTable){
        String className = class_instance.getParent().getChild(1).get("name");
        String varName = class_instance.getParent().getChild(0).get("name");
        List<String> imports = symTable.getImports();

        for(var local : symTable.getLocalVariables(currentMethod)){
            if(local.getName().equals(varName) && !local.getType().getName().equals(className)){
                var cenas = local.getName();
                var message = String.format("'%s' is not of type '%s'", varName, className);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(class_instance),
                        NodeUtils.getColumn(class_instance),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private Void visitVarDecl(JmmNode varDecl,SymbolTable symTable){
            var aa = varDecl.getChild(0);
            if (aa.get("name").equals("int...")){
                var message = String.format("%s cannot be declared as variable", aa.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }
        return null;
    }

}