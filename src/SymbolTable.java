import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public enum Kind { STATIC, FIELD, ARG, VAR, NONE }

    private static class Symbol {
        String type;
        Kind kind;
        int index;

        Symbol(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    private Map<String, Symbol> classScope = new HashMap<>();
    private Map<String, Symbol> subroutineScope = new HashMap<>();
    private Map<Kind, Integer> indices = new HashMap<>();

    public SymbolTable() {
        indices.put(Kind.STATIC, 0);
        indices.put(Kind.FIELD, 0);
        indices.put(Kind.ARG, 0);
        indices.put(Kind.VAR, 0);
    }

    public void startSubroutine() {
        subroutineScope.clear();
        indices.put(Kind.ARG, 0);
        indices.put(Kind.VAR, 0);
    }

    public void define(String name, String type, Kind kind) {
        int index = indices.get(kind);
        Symbol symbol = new Symbol(type, kind, index);
        indices.put(kind, index + 1);

        if (kind == Kind.STATIC || kind == Kind.FIELD) {
            classScope.put(name, symbol);
        } else if (kind == Kind.ARG || kind == Kind.VAR) {
            subroutineScope.put(name, symbol);
        }
    }

    public int varCount(Kind kind) {
        return indices.getOrDefault(kind, 0);
    }

    public Kind kindOf(String name) {
        if (subroutineScope.containsKey(name)) return subroutineScope.get(name).kind;
        if (classScope.containsKey(name)) return classScope.get(name).kind;
        return Kind.NONE;
    }

    public String typeOf(String name) {
        if (subroutineScope.containsKey(name)) return subroutineScope.get(name).type;
        if (classScope.containsKey(name)) return classScope.get(name).type;
        return null;
    }

    public int indexOf(String name) {
        if (subroutineScope.containsKey(name)) return subroutineScope.get(name).index;
        if (classScope.containsKey(name)) return classScope.get(name).index;
        return -1;
    }
}