package server;

import java.nio.channels.SelectionKey;
import java.util.Objects;

class WritingTask {
    public final SelectionKey key;
    public final boolean isWriteMode;
    public final String message;

    public WritingTask(SelectionKey key, boolean isWriteMode, String message) {
        this.key = Objects.requireNonNull(key, "Key containing socket cannot be null");
        this.isWriteMode = Objects.requireNonNull(isWriteMode, "Write mode must be specified");
        this.message = Objects.requireNonNull(message, "A message must be included");
    }
}
