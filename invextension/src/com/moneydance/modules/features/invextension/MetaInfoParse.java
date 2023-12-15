/*
 * MetaInfoParse.java
 * Copyright (c) 2023, Dale K. Furrow
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.moneydance.modules.features.invextension;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Displays Help File for user
 */


public final class MetaInfoParse {
    private static final String fileLocation = "/com/moneydance/modules/features/invextension/meta_info.dict";
    private LinkedHashMap<String, String> metaInfo;

    MetaInfoParse(){
        metaInfo = new LinkedHashMap<>();
        readDictFile();
    }

    private static String trimEntry(String entry){
        String replaced = entry.replace("\"", "").trim();
        return replaced;

    }

    private void readDictFile() {
        InputStream is = getClass().getResourceAsStream(fileLocation);
        String line;
        assert is != null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
            while ((line = br.readLine()) != null) {
                String[] arrOfStr = line.split("=", 2);
                if (arrOfStr.length == 2) {
                    metaInfo.put(trimEntry(arrOfStr[0]), trimEntry(arrOfStr[1]));
                }
            }
        } catch (Exception e){
            LogController.logException(e, "Exception on parsing meta info.");
        }
    }

    public void printMetaInfo() {
        for (Map.Entry<String, String> it : metaInfo.entrySet())
            System.out.print(it.getKey() + ", " + it.getValue() + "\n");
    }

    public LinkedHashMap<String, String> getMetaInfo() {
        return metaInfo;
    }

    public static void main(String[] args) {
        try {
            MetaInfoParse metaInfoParse = new MetaInfoParse();
            metaInfoParse.printMetaInfo();
            String module_build = metaInfoParse.getMetaInfo().get("module_build");
            System.out.println(module_build);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

