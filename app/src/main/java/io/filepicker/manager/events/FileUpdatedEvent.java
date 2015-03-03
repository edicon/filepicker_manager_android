package io.filepicker.manager.events;

import io.filepicker.manager.models.File;

/**
 * Created by maciejwitowski on 11/26/14.
 */
public final class FileUpdatedEvent {

    public final File file;

    public FileUpdatedEvent(File file){
        this.file = file;
    }
}
