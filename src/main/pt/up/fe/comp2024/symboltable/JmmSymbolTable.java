package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {

    //private final List<String> imports;
    private final String className;
    private final String superClass;
    private final List<String> methods;
    //private final List<Symbol> fields;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;


    public JmmSymbolTable(String className,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals,

                          String superClass/*,
                          List<Symbol> fields*/,
                          List<String> imports) {

        this.className = className;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
        //this.imports = imports;
        this.superClass = superClass;
        //this.fields = fields;
    }




    @Override
    public List<String> getImports() {
        throw new NotImplementedException();
    }



    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return this.superClass;
    }
    @Override
    public List<Symbol> getFields() {
        //return this.fields;
        throw new NotImplementedException();
    }
    @Override
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return this.returnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return Collections.unmodifiableList(params.get(methodSignature));
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.unmodifiableList(locals.get(methodSignature));
    }

}
