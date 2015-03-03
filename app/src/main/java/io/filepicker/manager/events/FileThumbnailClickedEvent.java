package io.filepicker.manager.events;

/**
 * Created by maciejwitowski on 12/1/14.
 */
public final class FileThumbnailClickedEvent {

    public final int position;

    public FileThumbnailClickedEvent(int position) {
        this.position = position;
    }
}
