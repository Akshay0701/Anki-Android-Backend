/*
 * Copyright (c) 2009 Daniel Svärd <daniel.svard@gmail.com>         (Utils.java)
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>               (Utils.java)
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>     (Utils.java)
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>   (Utils.java)
 * Copyright (c) 2014 Houssam Salem <houssam.salem.au@gmail.com>    (Shared.java)
 * Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>  (rsdroid/Shared.java)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This combines the works of
 * https://github.com/ankidroid/Anki-Android/blob/a711811eacddcb2a114bf18e6b92f22d06251b4c/AnkiDroid/src/main/java/com/ichi2/libanki/Utils.java
 * https://github.com/ankidroid/Anki-Android/blob/7a7ea56acba27fe538852700d4a4719659cff14f/AnkiDroid/src/androidTest/java/com/ichi2/anki/tests/Shared.java
 */

package net.ankiweb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.CheckResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class Shared {

    /**
     * Utility method to write to a file.
     * Throws the exception, so we can report it in syncing log
     */
    private static void writeToFileImpl(InputStream source, String destination) throws IOException {
        File f = new File(destination);
        try {
            f.createNewFile();

            @SuppressLint("DirectSystemCurrentTimeMillisUsage")
            long startTimeMillis = System.currentTimeMillis();
            long sizeBytes = CompatHelper.getCompat().copyFile(source, destination);
            @SuppressLint("DirectSystemCurrentTimeMillisUsage")
            long endTimeMillis = System.currentTimeMillis();

            long durationSeconds = (endTimeMillis - startTimeMillis) / 1000;
            long sizeKb = sizeBytes / 1024;
            long speedKbSec = 0;
            if (endTimeMillis != startTimeMillis) {
                speedKbSec = sizeKb * 1000 / (endTimeMillis - startTimeMillis);
            }
        } catch (IOException e) {
            throw new IOException(f.getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Calls {@link #writeToFileImpl(InputStream, String)} and handles IOExceptions
     * Does not close the provided stream
     * @throws IOException Rethrows exception after a set number of retries
     */
    public static void writeToFile(InputStream source, String destination) throws IOException {
        // sometimes this fails and works on retries (hardware issue?)
        final int retries = 5;
        int retryCnt = 0;
        boolean success = false;
        while (!success && retryCnt++ < retries) {
            try {
                writeToFileImpl(source, destination);
                success = true;
            } catch (IOException e) {
                if (retryCnt == retries) {
                    throw e;
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param name An additional suffix to ensure the test directory is only used by a particular resource.
     * @return See getTestDir.
     */
    private static File getTestDir(Context context, String name) {
        String suffix = "";
        if (!TextUtils.isEmpty(name)) {
            suffix = "-" + name;
        }
        File dir = new File(context.getCacheDir(), "testfiles" + suffix);
        if (!dir.exists()) {
            assertTrue(dir.mkdir());
        }
        for (File f : dir.listFiles()) {
            assertTrue(f.delete());
        }
        return dir;
    }

    /**
     * Copy a file from the application's assets directory and return the absolute path of that
     * copy.
     *
     * Files located inside the application's assets collection are not stored on the file
     * system and can not return a usable path, so copying them to disk is a requirement.
     */
    @CheckResult
    public static String getTestFilePath(Context context, String name) throws IOException {
        InputStream is = context.getClassLoader().getResourceAsStream("assets/" + name);
        if (is == null) {
            throw new FileNotFoundException("Could not find test file: assets/" + name);
        }
        String dst = new File(getTestDir(context, name), name).getAbsolutePath();
        writeToFile(is, dst);
        return dst;
    }
}
