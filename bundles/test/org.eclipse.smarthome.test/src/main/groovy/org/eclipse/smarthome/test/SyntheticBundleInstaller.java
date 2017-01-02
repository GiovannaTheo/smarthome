/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.config.xml.osgi.AbstractAsyncBundleProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.google.common.collect.ImmutableSet;

/**
 * Utility class for creation, installation, update and uninstallation of
 * synthetic bundles for the purpose of testing. The synthetic bundles content
 * should be stored into separate sub-directories of {@value #bundlePoolPath}
 * (which itself is situated in the test bundle's source directory). The
 * synthetic bundle is packed as a JAR and installed into the test runtime.
 * 
 * @author Alex Tugarev - Initial contribution
 * @author Dennis Nobel - Generalized the mechanism for creation of bundles by list of extensions to include
 * @author Simon Kaufmann - Install method returns when the bundle is fully loaded
 * @author Stefan Bussweiler - The list of extensions to include is extended with JSON
 * @author Andre Fuechsel - Implemented method for adding fragment
 * @author Kai Kreuzer - Applied formatting and license to the file
 * @author Dimitar Ivanov - The extension to include can be configured or default ones can be used; update method is
 *         introduced
 *
 */
public class SyntheticBundleInstaller {

    private static String bundlePoolPath = "/test-bundle-pool";

    /**
     * A list of default extensions to be included in the synthetic bundle.
     */
    private static Set<String> DEFAULT_EXTENSIONS = ImmutableSet.of("*.xml", "*.properties", "*.json", ".keep");

    /**
     * Install synthetic bundle, denoted by its name, into the test runtime (by using the given bundle context). Only
     * the default extensions set
     * ({@link #DEFAULT_EXTENSIONS}) will be included into the synthetic bundle
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param testBundleName the symbolic name of the sub-directory of {@value #bundlePoolPath}, which contains the
     *            files for the synthetic bundle
     * @return the synthetic bundle representation
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle
     */
    public static Bundle install(BundleContext bundleContext, String testBundleName) throws Exception {
        return install(bundleContext, testBundleName, DEFAULT_EXTENSIONS);
    }

    /**
     * Install synthetic bundle, denoted by its name, into the test runtime (by using the given bundle context).
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param testBundleNamethe symbolic name of the sub-directory of {@value #bundlePoolPath}, which contains the files
     *            for the synthetic bundle
     * @param extensionsToInclude a list of extension to be included into the synthetic bundle. In order to use the list
     *            of default extensions ({@link #DEFAULT_EXTENSIONS})
     * 
     * @return the synthetic bundle representation
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle
     */
    public static Bundle install(BundleContext bundleContext, String testBundleName, Set<String> extensionsToInclude)
            throws Exception {
        String bundlePath = bundlePoolPath + "/" + testBundleName + "/";
        byte[] syntheticBundleBytes = createSyntheticBundle(bundleContext.getBundle(), bundlePath, testBundleName,
                extensionsToInclude);

        Bundle syntheticBundle = bundleContext.installBundle(testBundleName,
                new ByteArrayInputStream(syntheticBundleBytes));
        syntheticBundle.start(Bundle.ACTIVE);
        waitUntilLoadingFinished(syntheticBundle);
        return syntheticBundle;
    }

    /**
     * Install synthetic bundle, denoted by its name, into the test runtime (by using the given bundle context).
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param testBundleName the symbolic name of the sub-directory of {@value #bundlePoolPath}, which contains the
     *            files for the synthetic bundle
     * @param extensionsToInclude a list of extension to be included into the synthetic bundle
     * 
     * @return the synthetic bundle representation
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle
     */
    public static Bundle install(BundleContext bundleContext, String testBundleName, String... extensionsToInclude)
            throws Exception {
        Set<String> extensionsSet = new HashSet<>(Arrays.asList(extensionsToInclude));
        return install(bundleContext, testBundleName, extensionsSet);
    }

    /**
     * Updates given bundle into the test runtime (the content is changed, but the symbolic name of the bundles remains
     * the same) with a new content, prepared in another resources directory.
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param bundleToUpdateName the symbolic name of the bundle to be updated
     * @param updateDirName the location of the new content, that the target bundle will be updated with
     * @return the Bundle representation of the updated bundle
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle
     */
    public static Bundle update(BundleContext bundleContext, String bundleToUpdateName, String updateDirName)
            throws Exception {
        return update(bundleContext, bundleToUpdateName, updateDirName, DEFAULT_EXTENSIONS);
    }

    /**
     * Updates given bundle into the test runtime (the content is changed, but the symbolic name of the bundles remains
     * the same) with a new content, prepared in another resources directory.
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param bundleToUpdateName the symbolic name of the bundle to be updated
     * @param updateDirName the location of the new content, that the target bundle will be updated with
     * @param extensionsToInclude a list of extension to be included into the synthetic bundle
     * @return the Bundle representation of the updated bundle
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle
     */
    public static Bundle update(BundleContext bundleContext, String bundleToUpdateName, String updateDirName,
            Set<String> extensionsToInclude) throws Exception {
        // Stop the bundle to update first
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundleToUpdateName.equals(bundle.getSymbolicName())) {
                // we have to uninstall the bundle to update its contents
                bundle.uninstall();
                break;
            }
        }

        // New bytes are taken from the update path
        String updatePath = bundlePoolPath + "/" + updateDirName + "/";
        byte[] updatedBundleBytes = createSyntheticBundle(bundleContext.getBundle(), updatePath, bundleToUpdateName,
                extensionsToInclude);

        // The updated bytes are installed with the same name
        Bundle syntheticBundle = bundleContext.installBundle(bundleToUpdateName,
                new ByteArrayInputStream(updatedBundleBytes));

        // Starting the bundle
        syntheticBundle.start(Bundle.ACTIVE);
        waitUntilLoadingFinished(syntheticBundle);
        return syntheticBundle;
    }

    /**
     * Updates given bundle into the test runtime (the content is changed, but the symbolic name of the bundles remains
     * the same) with a new content, prepared in another resources directory.
     * 
     * @param bundleContextthe bundle context of the test runtime
     * @param bundleToUpdateName the symbolic name of the bundle to be updated
     * @param updateDirName the location of the new content, that the target bundle will be updated with
     * @param extensionsToInclude a list of extension to be included into the synthetic bundle
     * @return the Bundle representation of the updated bundle
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle
     */
    public static Bundle update(BundleContext bundleContext, String bundleToUpdateName, String updateDirName,
            String... extensionsToInclude) throws Exception {
        Set<String> extensionsSet = new HashSet<>(Arrays.asList(extensionsToInclude));
        return update(bundleContext, bundleToUpdateName, updateDirName, extensionsSet);
    }

    /**
     * Install synthetic bundle fragment, denoted by its name, into the test
     * runtime (by using the given bundle context). Only the default extensions
     * set ({@link #DEFAULT_EXTENSIONS}) will be included into the synthetic
     * bundle fragment.
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param testBundleName the name of the sub-directory of {@value #bundlePoolPath}, which contains the files for the
     *            synthetic bundle
     * @param extensionsToInclude a list of extension to be included into the synthetic bundle fragment. In order to use
     *            the list of default extensions ({@link #DEFAULT_EXTENSIONS})
     * @return the synthetic bundle representation
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle fragment
     */
    public static Bundle installFragment(BundleContext bundleContext, String testBundleName) throws Exception {
        return installFragment(bundleContext, testBundleName, DEFAULT_EXTENSIONS);
    }

    /**
     * Install synthetic bundle fragment, denoted by its name, into the test runtime (by using the given bundle
     * context). Only the default extensions set ({@link #DEFAULT_EXTENSIONS}) will be included into the synthetic
     * bundle fragment.
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param testBundleName the name of the sub-directory of {@value #bundlePoolPath}, which contains the files for the
     *            synthetic bundle
     * @return the synthetic bundle representation
     * @throws Exception thrown when error occurs while installing or starting the synthetic bundle fragment
     */
    public static Bundle installFragment(BundleContext bundleContext, String testBundleName,
            Set<String> extensionsToInclude) throws Exception {
        String bundlePath = bundlePoolPath + "/" + testBundleName + "/";
        byte[] syntheticBundleBytes = createSyntheticBundle(bundleContext.getBundle(), bundlePath, testBundleName,
                extensionsToInclude);

        Bundle syntheticBundle = bundleContext.installBundle(testBundleName,
                new ByteArrayInputStream(syntheticBundleBytes));
        waitUntilLoadingFinished(syntheticBundle);
        return syntheticBundle;
    }

    /**
     * Explicitly wait for the given bundle to finish its loading
     * 
     * @param bundle the bundle object representation
     */
    public static void waitUntilLoadingFinished(Bundle bundle) {
        while (!AbstractAsyncBundleProcessor.isBundleFinishedLoading(bundle)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Uninstalls the synthetic bundle (or bundle fragment), denoted by its name, from the test runtime.
     * 
     * @param bundleContext the bundle context of the test runtime
     * @param testBundleName the name of the test bundle to be uninstalled
     * @throws BundleException if error is met during the bundle uninstall
     */
    public static void uninstall(BundleContext bundleContext, String testBundleName) throws BundleException {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (testBundleName.equals(bundle.getSymbolicName())) {
                bundle.uninstall();
            }
        }
    }

    private static byte[] createSyntheticBundle(Bundle bundle, String bundlePath, String bundleName,
            Set<String> extensionsToInclude) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Manifest manifest = getManifest(bundle, bundlePath);
        JarOutputStream jarOutputStream = manifest != null ? new JarOutputStream(outputStream, manifest)
                : new JarOutputStream(outputStream);

        List<String> files = collectFilesFrom(bundle, bundlePath, bundleName, extensionsToInclude);
        for (String file : files) {
            addFileToArchive(bundle, bundlePath, file, jarOutputStream);
        }
        jarOutputStream.close();
        return outputStream.toByteArray();
    }

    private static void addFileToArchive(Bundle bundle, String bundlePath, String fileInBundle,
            JarOutputStream jarOutputStream) throws IOException {
        String filePath = bundlePath + "/" + fileInBundle;
        URL resource = bundle.getResource(filePath);
        if (resource == null) {
            return;
        }
        ZipEntry zipEntry = new ZipEntry(fileInBundle);
        jarOutputStream.putNextEntry(zipEntry);
        IOUtils.copy(resource.openStream(), jarOutputStream);
        jarOutputStream.closeEntry();
    }

    private static List<String> collectFilesFrom(Bundle bundle, String bundlePath, String bundleName,
            Set<String> extensionsToInclude) throws Exception {
        List<String> result = new ArrayList<>();
        URL url = getBaseURL(bundle, bundleName);
        if (url != null) {
            String path = url.getPath();
            URI baseURI = url.toURI();

            List<URL> list = collectEntries(bundle, path, extensionsToInclude);
            for (URL entryURL : list) {
                String fileEntry = convertToFileEntry(baseURI, entryURL);
                result.add(fileEntry);
            }
        }
        return result;
    }

    private static URL getBaseURL(Bundle bundle, String bundleName) {
        Enumeration<URL> entries = bundle.findEntries("/", bundleName, true);
        return entries != null ? entries.nextElement() : null;
    }

    private static List<URL> collectEntries(Bundle bundle, String path, Set<String> extensionsToInclude) {
        List<URL> result = new ArrayList<>();
        for (String filePattern : extensionsToInclude) {
            Enumeration<URL> entries = bundle.findEntries(path, filePattern, true);
            if (entries != null) {
                result.addAll(Collections.list(entries));
            }
        }
        return result;
    }

    private static String convertToFileEntry(URI baseURI, URL entryURL) throws URISyntaxException {
        URI entryURI = entryURL.toURI();
        URI relativeURI = baseURI.relativize(entryURI);
        String fileEntry = relativeURI.toString();
        return fileEntry;
    }

    private static Manifest getManifest(Bundle bundle, String bundlePath) throws IOException {
        String filePath = bundlePath + "/" + "META-INF/MANIFEST.MF";
        URL resource = bundle.getResource(filePath);
        if (resource == null) {
            return null;
        }
        return new Manifest(resource.openStream());
    }
}