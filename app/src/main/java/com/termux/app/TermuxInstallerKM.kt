package com.termux.app

import android.app.Activity
import android.os.Build
import android.system.Os
import android.util.Log
import android.util.Pair
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object TermuxInstallerKM {
    private const val TAG = "TermuxInstallerKM"
    /** Performs setup if necessary.  */
    fun setupIfNeeded(activity: Activity, whenDone: Runnable) {
        val PREFIX_FILE = File(Config.PREFIX_PATH)
        if (PREFIX_FILE.isDirectory) {
            PREFIX_FILE.deleteOnExit()
        }
        object : Thread() {
            override fun run() {
                try {
                    val STAGING_PREFIX_PATH = Config.FILES_PATH + "/usr-staging"
                    val STAGING_PREFIX_FILE = File(STAGING_PREFIX_PATH)
                    if (STAGING_PREFIX_FILE.exists()) {
                        deleteFolder(STAGING_PREFIX_FILE)
                    }
                    val buffer = ByteArray(8096)
                    val symlinks: MutableList<Pair<String, String>> = ArrayList(50)
                    val zipUrl = determineZipUrl()
                    //FIXME  at java we like use while loop for processing( copy/unzip/...) file
                    //But kotlin  does not like use while !!  can give us the better example for copy/unzip file ?
                    // or any Syntactic sugar ?
                    ZipInputStream(zipUrl.openStream()).use { zipInput ->
                        //FIXME Here we modify  it can run
                        var zipEntry: ZipEntry?
                        while (zipInput.nextEntry.also { zipEntry = it } != null) {
                            if (zipEntry?.name == "SYMLINKS.txt") {
                                //FIXME  here kotlin must modity  if use java auoto-convert to Kotlin . if not will error
                                BufferedReader(InputStreamReader(zipInput, StandardCharsets.UTF_8)).lines().forEach { line ->
                                    //FIXME modify but bad !!!  lines and forEach must API over 24
                                    if (line.isNullOrBlank()) return@forEach  //FIXME we modify if not  the SYMLINKS get blank -->error
                                    val parts = line.split("←").toTypedArray()
                                    if (parts.size != 2) {
                                        throw RuntimeException("Malformed symlink line: $line")
                                    }
                                    val oldPath = parts[0]
                                    val newPath = STAGING_PREFIX_PATH + "/" + parts[1]
                                    symlinks.add(Pair.create(oldPath, newPath))
                                    ensureDirectoryExists(File(newPath).parentFile)
                                }
                            } else {
                                val zipEntryName = zipEntry?.name
                                val targetFile = File(STAGING_PREFIX_PATH, zipEntryName)
                                val isDirectory = zipEntry!!.isDirectory
                                ensureDirectoryExists(if (isDirectory) targetFile else targetFile.parentFile)
                                if (!isDirectory) {
                                    FileOutputStream(targetFile).use { outStream ->
                                        var readBytes: Int
                                        while (zipInput.read(buffer).also { readBytes = it } != -1) outStream.write(buffer, 0, readBytes)
                                    }
                                    if (zipEntryName!!.startsWith("bin/") || zipEntryName!!.startsWith("libexec") || zipEntryName!!.startsWith("lib/apt/methods")) {
                                        Os.chmod(targetFile.absolutePath, 448)
                                    }
                                }
                            }
                        }
                    }
                    if (symlinks.isEmpty()) throw RuntimeException("No SYMLINKS.txt encountered")
                    for (symlink in symlinks) {
                        Os.symlink(symlink.first, symlink.second)
                    }
                    if (!STAGING_PREFIX_FILE.renameTo(PREFIX_FILE)) {
                        throw RuntimeException("Unable to rename staging folder")
                    }
//                    activity.runOnUiThread(whenDone)
                } catch (e: Exception) {
                    Log.e(TAG, "Bootstrap error", e)
                } finally {
                }
            }
        }.start()
    }

    private fun ensureDirectoryExists(directory: File) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw RuntimeException("Unable to create directory: " + directory.absolutePath)
        }
    }

    /** Get bootstrap zip url for this systems cpu architecture.  */
    @Throws(MalformedURLException::class)
    private fun determineZipUrl(): URL {
        val archName = determineTermuxArchName()
        val url = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) "https://bintray.com/termux/bootstrap/download_file?file_path=bootstrap-$archName-v18.zip" else "https://termux.net/bootstrap/bootstrap-$archName.zip"
        return URL(url)
    }

    private fun determineTermuxArchName(): String { // Note that we cannot use System.getProperty("os.arch") since that may give e.g. "aarch64"
// while a 64-bit runtime may not be installed (like on the Samsung Galaxy S5 Neo).
// Instead we search through the supported abi:s on the device, see:
// http://developer.android.com/ndk/guides/abis.html
// Note that we search for abi:s in preferred order (the ordering of the
// Build.SUPPORTED_ABIS list) to avoid e.g. installing arm on an x86 system where arm
// emulation is available.
        for (androidArch in Build.SUPPORTED_ABIS) {
            when (androidArch) {
                "arm64-v8a" -> return "aarch64"
                "armeabi-v7a" -> return "arm"
                "x86_64" -> return "x86_64"
                "x86" -> return "i686"
            }
        }
        throw RuntimeException("Unable to determine arch from Build.SUPPORTED_ABIS =  " +
            Arrays.toString(Build.SUPPORTED_ABIS))
    }

    /** Delete a folder and all its content or throw. Don't follow symlinks.  */
    @Throws(IOException::class)
    fun deleteFolder(fileOrDirectory: File) {
        if (fileOrDirectory.canonicalPath == fileOrDirectory.absolutePath && fileOrDirectory.isDirectory) {
            val children = fileOrDirectory.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteFolder(child)
                }
            }
        }
        if (!fileOrDirectory.delete()) {
            throw RuntimeException("Unable to delete " + (if (fileOrDirectory.isDirectory) "directory " else "file ") + fileOrDirectory.absolutePath)
        }
    }
}
