package com.ae2utilix.util;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UnionMachineSet implements IMachineSet {

    private final Class<? extends IGridHost> machineClass;
    private final List<IMachineSet> sets = new ArrayList<>();
    private int count;

    public UnionMachineSet(Class<? extends IGridHost> machineClass, IMachineSet... sets) {
        this.machineClass = machineClass;
        this.count = 0;
        for (IMachineSet set : sets) {
            if (!set.isEmpty()) {
                this.count += set.size();
                this.sets.add(set);
            }
        }
    }

    @Nonnull
    @Override
    public Class<? extends IGridHost> getMachineClass() {
        return machineClass;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        for (IMachineSet set : sets) {
            if (!set.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean contains(Object o) {
        for (IMachineSet set : sets) {
            if (set.contains(o)) return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public Iterator<IGridNode> iterator() {
        return new UnionIterator();
    }

    private class UnionIterator implements Iterator<IGridNode> {
        private final List<Iterator<IGridNode>> iterators = new ArrayList<>();
        private int current = 0;

        UnionIterator() {
            for (IMachineSet set : sets) {
                iterators.add(set.iterator());
            }
        }

        @Override
        public boolean hasNext() {
            while (current < iterators.size()) {
                if (iterators.get(current).hasNext()) return true;
                current++;
            }
            return false;
        }

        @Override
        public IGridNode next() {
            return iterators.get(current).next();
        }
    }
}
