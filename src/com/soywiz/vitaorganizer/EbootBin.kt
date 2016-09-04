package com.soywiz.vitaorganizer

import com.soywiz.util.Stream2

object EbootBin {
    fun hasExtendedPermissions(s: Stream2): Boolean {
        val s2 = s.slice()
        s2.position = 0x80
        val authid = s2.readS64_le()
        return when (authid) {
            0x2F00000000000001L, 0x2F00000000000003L -> true
            else -> false
        }
    }

    /*
    // Check permissions
	char path[MAX_PATH_LENGTH];
	snprintf(path, MAX_PATH_LENGTH, "%s/eboot.bin", args->file);
	SceUID fd = archiveFileOpen(path, SCE_O_RDONLY, 0);
	if (fd >= 0) {
		char buffer[0x88];
		archiveFileRead(fd, buffer, sizeof(buffer));
		archiveFileClose(fd);

		// Team molecule's request: Full permission access warning
		uint64_t authid = *(uint64_t *)(buffer + 0x80);
		if (authid == 0x2F00000000000001 || authid == 0x2F00000000000003) {
			closeWaitDialog();

			initMessageDialog(SCE_MSG_DIALOG_BUTTON_TYPE_YESNO, language_container[INSTALL_WARNING]);
			dialog_step = DIALOG_STEP_INSTALL_WARNING;

			// Wait for response
			while (dialog_step == DIALOG_STEP_INSTALL_WARNING) {
				sceKernelDelayThread(1000);
			}

			// Cancelled
			if (dialog_step == DIALOG_STEP_CANCELLED) {
				closeWaitDialog();
				goto EXIT;
			}

			// Init again
			initMessageDialog(MESSAGE_DIALOG_PROGRESS_BAR, language_container[INSTALLING]);
			dialog_step = DIALOG_STEP_INSTALLING;
		}
	}
    */
}