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
import java.util.function.BiFunction;

public class CheckMethod extends AnalysisVisitor {
    public String currentClass;
    public String currentMethod;
    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
    }

    private Void visitClassDecl(JmmNode currClass, SymbolTable symTable){
        currentClass = currClass.get("name");
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodExpr(JmmNode methodExpr, SymbolTable symTable){
        /*
        boolean exists = false;
        for (var imports: symTable.getImports()) {
            if (imports.equals(methodExpr.getChild(0).get("name"))) {
                exists = true;
                break;
            }
        }
        List<String> methods = symTable.getMethods();
        var expr = methodExpr;
        String name = expr.getChild(0).get("name");
        if (// validar imports e lcocals){
            //erro
        }
        expr = expr.getChild(1);
        while(!expr.getKind().equals("CallMethod")){
            name = expr.getChild(0).get("name");
            if (!methods.contains(name)){
                //erro
            }

            expr = expr.getChild(1);
        }


    }

         */
    return null;
}
}
