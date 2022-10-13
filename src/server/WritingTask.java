package server;

import java.nio.channels.SelectionKey;

public class WritingTask {
    public final SelectionKey key;
    public final boolean isWriteMode;
    public final String message;

    public WritingTask(SelectionKey key, boolean isWriteMode, String message) {
        this.key = key;
        this.isWriteMode = isWriteMode;
        this.message = message;
    }
}
