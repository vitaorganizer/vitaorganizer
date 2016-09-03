package com.soywiz.vitaorganizer

import it.sauronsoftware.ftp4j.FTPClient
import it.sauronsoftware.ftp4j.FTPDataTransferListener
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

object PsvitaDevice {

    val ftp = FTPClient()
    var ip = "192.168.1.130"
    var port = 1337

    fun setIp(ip: String, port: Int = 1337) {
        this.ip = ip
        this.port = port
    }

    private fun connectedFtp(): FTPClient {
        if (!ftp.isConnected) {
            ftp.connect(ip, port)
            ftp.login("", "")
        }
        return ftp
    }

    fun getGameIds() = connectedFtp().list("/ux0:/app").filter { i -> i.type == it.sauronsoftware.ftp4j.FTPFile.TYPE_DIRECTORY }.map { File(it.name).name }

    fun getGameFolder(id: String) = "/ux0:/app/${File(id).name}"

    fun downloadSmallFile(path: String): ByteArray {
        val file = File.createTempFile("vita", "download")
        connectedFtp().download(path, file)
        val data = file.readBytes()
        file.delete()
        return data
    }

    fun getParamSfo(id: String): ByteArray = downloadSmallFile("${getGameFolder(id)}/sce_sys/param.sfo")
    fun getGameIcon(id: String): ByteArray = downloadSmallFile("${getGameFolder(id)}/sce_sys/icon0.png")

    fun uploadGame(id: String, zip: ZipFile) {
        val base = getGameFolder(id)

        val createDirectoryCache = hashSetOf<String>()

        fun createDirectories(path: String) {
            val parent = File(path).parent
            if (parent != "" && parent != null) {
                createDirectories(parent)
            }
            if (path !in createDirectoryCache) {
                println("Creating directory $path...")
                createDirectoryCache.add(path)
                try {
                    connectedFtp().createDirectory(path)
                } catch (e: IOException) {
                    throw e
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        for (entry in zip.entries()) {
            val vname = "$base/${entry.name}"
            val directory = File(vname).parent
            println("Writting $vname...")
            if (!entry.isDirectory) {
                createDirectories(directory)
                connectedFtp().upload(vname, zip.getInputStream(entry), 0L, 0L, object : FTPDataTransferListener {
                    override fun started() {
                    }

                    override fun completed() {
                    }

                    override fun aborted() {
                    }

                    override fun transferred(p0: Int) {
                    }

                    override fun failed() {
                    }
                })
            }
        }

        println("DONE. Now package should be promoted!")
    }

    /*
    fun writePackage() {
        val PACKAGE_PARENT = "ux0:ptmp"
        val PACKAGE_DIR = "$PACKAGE_PARENT/pkg"
        val HEAD_BIN = "$PACKAGE_DIR/sce_sys/package/head.bin"
        // promote(PACKAGE_DIR)
    }
    */

    /*

void loadScePaf() {
	uint32_t ptr[0x100] = { 0 };
	ptr[0] = 0;
	ptr[1] = (uint32_t)&ptr[0];
	uint32_t scepaf_argp[] = { 0x400000, 0xEA60, 0x40000, 0, 0 };
	sceSysmoduleLoadModuleInternalWithArg(0x80000008, sizeof(scepaf_argp), scepaf_argp, ptr);
}

int promote(char *path) {
	int res;

	loadScePaf();

	res = sceSysmoduleLoadModuleInternal(SCE_SYSMODULE_PROMOTER_UTIL);
	res = scePromoterUtilityInit();
	res = scePromoterUtilityPromotePkg(path, 0);

	int state = 0;
	do {
		res = scePromoterUtilityGetState(&state);
		sceKernelDelayThread(300 * 1000);
	} while (state);

	int result = 0;
	res = scePromoterUtilityGetResult(&result);
	res = scePromoterUtilityExit();
	res = sceSysmoduleUnloadModuleInternal(SCE_SYSMODULE_PROMOTER_UTIL);
	return result;
}


	// Make head.bin
	res = makeHeadBin();
	if (res < 0) {
		closeWaitDialog();
		errorDialog(res);
		goto EXIT;
	}

	// Promote
	res = promote(PACKAGE_DIR);
	if (res < 0) {
		closeWaitDialog();
		errorDialog(res);
		goto EXIT;
	}

    int makeHeadBin() {
        uint8_t hmac[16];
        uint32_t off;
        uint32_t len;
        uint32_t out;

        // head.bin must not be present
        SceIoStat stat;
        memset(&stat, 0, sizeof(SceIoStat));
        if (sceIoGetstat(HEAD_BIN, &stat) >= 0)
            return -1;

        // Get title id
        char *title_id = get_title_id(PACKAGE_DIR "/sce_sys/param.sfo");
        if (!title_id)// || strlen(title_id) != 9) // Enforce TITLEID format?
            return -2;

        // Allocate head.bin buffer
        uint8_t *head_bin = malloc(sizeof(base_head_bin));
        memcpy(head_bin, base_head_bin, sizeof(base_head_bin));

        // Write full titleid
        char full_title_id[128];
        snprintf(full_title_id, sizeof(full_title_id), "EP9000-%s_00-XXXXXXXXXXXXXXXX", title_id);
        strncpy((char *)&head_bin[0x30], full_title_id, 48);

        // hmac of pkg header
        len = ntohl(*(uint32_t *)&head_bin[0xD0]);
        fpkg_hmac(&head_bin[0], len, hmac);
        memcpy(&head_bin[len], hmac, 16);

        // hmac of pkg info
        off = ntohl(*(uint32_t *)&head_bin[0x8]);
        len = ntohl(*(uint32_t *)&head_bin[0x10]);
        out = ntohl(*(uint32_t *)&head_bin[0xD4]);
        fpkg_hmac(&head_bin[off], len - 64, hmac);
        memcpy(&head_bin[out], hmac, 16);

        // hmac of everything
        len = ntohl(*(uint32_t *)&head_bin[0xE8]);
        fpkg_hmac(&head_bin[0], len, hmac);
        memcpy(&head_bin[len], hmac, 16);

        // Make dir
        sceIoMkdir(PACKAGE_DIR "/sce_sys/package", 0777);

        // Write head.bin
        WriteFile(HEAD_BIN, head_bin, sizeof(base_head_bin));

        free(head_bin);
        free(title_id);

        return 0;
    }

     */
}