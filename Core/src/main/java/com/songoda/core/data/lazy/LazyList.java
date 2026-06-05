package com.songoda.core.data.lazy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class LazyList<T> {
    private final List<T> list;
    private List<Lazy<T>> lazyList;

    public LazyList(List<T> list) {
        this.list = list == null ? new LinkedList<>() : list;
        this.lazyList = new LinkedList<>();
    }

    public List<T> getList() {
        loadList();
        return this.list;
    }

    public Set<T> getSet() {
        loadList();
        return new HashSet<>(this.list);
    }

    public void add(Supplier<T> supplier) {
        if (this.lazyList == null) {
            this.list.add(supplier.get());
        } else if (supplier != null) {
            this.lazyList.add(new Lazy<T>().set(supplier));
        }
    }

    public void add(T items) {
        this.list.add(items);
    }

    public void addAll(List<T> items) {
        this.list.addAll(items);
    }

    public void addAll(Supplier<T>... suppliers) {
        for (Supplier<T> supplier : suppliers) {
            add(supplier);
        }
    }

    private void loadList() {
        if (this.lazyList == null) {
            return;
        }

        for (Lazy<T> lazy : this.lazyList) {
            this.list.add(lazy.get());
        }
        this.lazyList = null;
    }

    public void clear() {
        this.list.clear();
        this.lazyList.clear();
    }

    public boolean isEmpty() {
        if (this.lazyList != null) {
            return this.lazyList.isEmpty();
        }
        return this.list.isEmpty();
    }
}
