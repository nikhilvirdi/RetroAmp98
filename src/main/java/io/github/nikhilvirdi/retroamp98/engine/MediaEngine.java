package io.github.nikhilvirdi.retroamp98.engine;

import java.nio.file.Path;
import java.time.Duration;

public interface MediaEngine extends AutoCloseable {

    @Override
    default void close() {
        stop();
    }

    void load(Path path);

    void play();

    void pause();

    void stop();

    void seek(Duration position);

    void setVolume(double volume);

    void setMuted(boolean muted);

    PlayerState getState();

    Duration getPosition();

    Duration getDuration();

    double getVolume();

    boolean isMuted();

    void addListener(EngineListener listener);

    void removeListener(EngineListener listener);
}
