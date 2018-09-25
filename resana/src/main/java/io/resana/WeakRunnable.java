package io.resana;

import java.lang.ref.WeakReference;

abstract class WeakRunnable<T> implements Runnable {
    WeakReference<T> ref;

    WeakRunnable(T ref) {
        this.ref = new WeakReference<T>(ref);
    }

    @Override
    public void run() {
        T object = ref.get();
        if (object != null)
            run(object);
    }

    abstract void run(T object);
}