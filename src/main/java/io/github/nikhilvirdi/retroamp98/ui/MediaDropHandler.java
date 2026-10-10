package io.github.nikhilvirdi.retroamp98.ui;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

/**
 * Drag-and-drop handler for receiving audio/video files, directories, and playlists.
 */
public final class MediaDropHandler {

    private MediaDropHandler() {
    }

    /**
     * Installs a DropTarget onto the specified component that routes dropped files to the controller.
     */
    public static void install(Component component, PlayerController controller) {
        if (component == null || controller == null) return;
        new DropTarget(component, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable t = dtde.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            controller.handleDroppedFiles(files);
                            dtde.dropComplete(true);
                            return;
                        }
                    }
                    dtde.dropComplete(false);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        });
    }
}
