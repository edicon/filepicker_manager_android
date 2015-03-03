package io.filepicker.manager.events;

import io.filepicker.manager.models.File;

/**
 * Created by maciejwitowski on 11/6/14.
 */
public final class FileSavedEvent {

    public final File file;
    public final String operationType;

    public FileSavedEvent(File file, String operationType) {

        this.file = file;
        this.operationType = operationType;
    }
}
