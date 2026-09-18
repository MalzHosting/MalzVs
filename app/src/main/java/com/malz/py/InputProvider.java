package com.malz.py;

public class InputProvider {

    public interface Listener {
        void onInputRequested(String prompt);
    }

    private final Listener listener;
    private boolean waiting = false;
    private String value = "";

    public InputProvider(Listener listener) {
        this.listener = listener;
    }

    public synchronized String readLine(String prompt) {
        waiting = true;
        value = "";

        listener.onInputRequested(prompt);

        while (waiting) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
        }

        return value;
    }

    public synchronized void submit(String text) {
        value = text;
        waiting = false;
        notifyAll();
    }

    public synchronized void cancel() {
        value = "";
        waiting = false;
        notifyAll();
    }
}
