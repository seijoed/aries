/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class FragmentBuilder {
    private List<String> importPackages = new ArrayList<String>();
    private List<String> exportPackages = new ArrayList<String>();
    private Bundle hostBundle;
    private String nameExtension;
    private String bundleNameExtension;
    private String fragmentName;
    private Map<String, byte[]> files = new HashMap<String, byte[]>();

    public FragmentBuilder(Bundle host) {
        this(host, ".fragment", "Fragment");
    }
    
    public FragmentBuilder(Bundle host, String symbolicNameSuffix, String bundleNameSuffix) {
        hostBundle = host;
        nameExtension = symbolicNameSuffix;
        bundleNameExtension = bundleNameSuffix;

        // make sure we have an initial '.'
        if (!!!nameExtension.startsWith(".")) {
            nameExtension = "." + nameExtension;
        }
    }

    public void setName(String name) {
        fragmentName = name;
    }

    public void addImports(String... imports) {
        importPackages.addAll(Arrays.asList(imports));
    }

    public void addExports(String... imports) {
        exportPackages.addAll(Arrays.asList(imports));
    }

    public void addImportsFromExports(Bundle exportBundle) {
        String exportString = (String) exportBundle.getHeaders().get(Constants.EXPORT_PACKAGE);

        if (exportString != null) {
            String exportVersion = exportBundle.getVersion().toString();
            String bundleConstraint = Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE
                    + "=\"" + exportBundle.getSymbolicName() + "\"";
            String bundleVersionConstraint = Constants.BUNDLE_VERSION_ATTRIBUTE
                    + "=\"[" + exportVersion + "," + exportVersion + "]\"";

            List<String> exports = parseDelimitedString(exportString, ",", true);            
            for (String export : exports) {
                importPackages.add(convertExportToImport(export, bundleConstraint, bundleVersionConstraint));
            }
        }
    }

    /**
     * Filter out directives in the export statement
     * 
     * @param exportStatement
     * @return
     */
    private String convertExportToImport(String exportStatement,
                                         String bundleConstraint, 
                                         String bundleVersionConstraint) {
        StringBuffer result = new StringBuffer();

        for (String fragment : exportStatement.split("\\s*;\\s*")) {
            int pos = fragment.indexOf('=');

            // similar to fragment.contains(":=") but looks for the first '='
            // and checks whether this is part of ':='
            // in this way we will not be fooled by attributes like
            // a="something:=strange"
            if (!!!(pos > 0 && fragment.charAt(pos - 1) == ':')) {
                result.append(fragment);
                result.append(';');
            }
        }

        result.append(bundleConstraint);
        result.append(';');
        result.append(bundleVersionConstraint);

        return result.toString();
    }

    public void addFile(String path, byte[] content) {
        files.put(path, content);
    }

    public Bundle install(BundleContext ctx) throws IOException, BundleException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = null;

        try {
            jos = new JarOutputStream(baos, makeManifest());
            addFileContent(jos);
        } finally {
            if (jos != null)
                jos.close();
            baos.close();
        }

        byte[] inMemoryJar = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(inMemoryJar);

        return ctx.installBundle(getFragmentSymbolicName(), bais);
    }

    private void addFileContent(JarOutputStream jos) throws IOException {
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            jos.putNextEntry(new JarEntry(entry.getKey()));
            jos.write(entry.getValue());
        }
    }

    public String getFragmentSymbolicName() {
        return hostBundle.getSymbolicName() + nameExtension;
    }

    public String getFragmentBundleName() {
        if (fragmentName != null) {
            return fragmentName;
        } else {
            String bundleName = (String) hostBundle.getHeaders().get(Constants.BUNDLE_NAME);
            if (bundleName != null && bundleNameExtension != null) {
                return bundleName.trim() + " " + bundleNameExtension.trim();
            }
        }
        return null;
    }
    
    private Manifest makeManifest() {
        String commonVersion = hostBundle.getVersion().toString();
        String fragmentHost = hostBundle.getSymbolicName() + ";"
                + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"" + commonVersion
                + "\"";

        Manifest m = new Manifest();
        Attributes manifestAttributes = m.getMainAttributes();
        manifestAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        manifestAttributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        manifestAttributes.putValue(Constants.BUNDLE_SYMBOLICNAME, getFragmentSymbolicName());   
        
        String bundleName = getFragmentBundleName();        
        if (bundleName != null) {
            manifestAttributes.putValue(Constants.BUNDLE_NAME, bundleName);
        }
            
        manifestAttributes.putValue(Constants.BUNDLE_VERSION, commonVersion);
        manifestAttributes.putValue(Constants.BUNDLE_VENDOR, "Apache");
        manifestAttributes.putValue(Constants.FRAGMENT_HOST, fragmentHost);

        addImportsAndExports(manifestAttributes);

        return m;
    }

    private void addImportsAndExports(Attributes attrs) {
        if (!!!importPackages.isEmpty()) {
            attrs.putValue(Constants.IMPORT_PACKAGE, joinStrings(importPackages, ','));
        }

        if (!!!exportPackages.isEmpty()) {
            attrs.putValue(Constants.EXPORT_PACKAGE, joinStrings(exportPackages, ','));
        }
    }

    private String joinStrings(List<String> strs, char separator) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String str : strs) {
            if (first)
                first = false;
            else
                result.append(separator);

            result.append(str);
        }

        return result.toString();
    }
    
    private static List<String> parseDelimitedString(String value, String delim, boolean includeQuotes) {   
        if (value == null) {       
            value = "";
        }

        List<String> list = new ArrayList<String>();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        for (int i = 0; i < value.length(); i++) {        
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);
            boolean isQuote = (c == '"');

            if (isDelimiter && ((expecting & DELIMITER) > 0)) {            
                list.add(sb.toString().trim());
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            } else if (isQuote && ((expecting & STARTQUOTE) > 0)) { 
                if (includeQuotes) {
                    sb.append(c);
                }
                expecting = CHAR | ENDQUOTE;
            } else if (isQuote && ((expecting & ENDQUOTE) > 0)) {    
                if (includeQuotes) {
                    sb.append(c);
                }
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            } else if ((expecting & CHAR) > 0) {            
                sb.append(c);
            } else {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }
        }

        if (sb.length() > 0) {        
            list.add(sb.toString().trim());
        }

        return list;
    }
}
