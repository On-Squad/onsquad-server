package revi1337.onsquad.inrastructure.file.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RecycleBin {

    private static final Queue<String> RECYCLE_BIN = new ConcurrentLinkedQueue<>();

    public static void append(String imageUrl) {
        RECYCLE_BIN.add(imageUrl);
    }

    public static List<String> flush() {
        List<String> removed = new ArrayList<>();
        String item;
        while ((item = RECYCLE_BIN.poll()) != null) {
            removed.add(item);
        }
        return removed;
    }
}
