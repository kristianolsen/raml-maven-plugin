package no.trinnvis;

import java.util.LinkedHashSet;
import java.util.Set;

class Tree<T> implements Visitable<T> {

    // NB: LinkedHashSet preserves insertion order
    private final Set<Tree> children = new LinkedHashSet<>();
    private final T data;

    Tree(T data) {
        this.data = data;
    }

    public void accept(Visitor<T> visitor) {
        visitor.visitData(this, data);

        for (Tree child : children) {
            Visitor<T> childVisitor = visitor.visitTree(child);
            child.accept(childVisitor);
        }
    }

    Tree<T> child(T data) {
        for (Tree child: children ) {
            if (child.data.equals(data)) {
                return child;
            }
        }

        return child(new Tree<>(data));
    }

    Tree child(Tree<T> child) {
        children.add(child);
        return child;
    }
}

