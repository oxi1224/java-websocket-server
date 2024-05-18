package io.github.oxi1224.websocket.shared.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Contains differnet methods used for scanning classes accross the project
 */
public class ClassScanner {
  /**
   * Finds and returns all classes with the given annotation
   * @param annotation - The annotation to search for
   * @param packageName - The package to search
   * @return all found classes
   */
  public static List<Class<?>> findAllWithAnnotation(Class<? extends Annotation> annotation, String packageName) {
    List<Class<?>> withAnnotation = new ArrayList<>();
    try {
      List<Class<?>> classes = getClasses(packageName);
      for (Class<?> c : classes) {
        if (c.isAnnotationPresent(annotation)) withAnnotation.add(c);
      }
    } catch (ClassNotFoundException | IOException e) {
      e.printStackTrace();
    }
    return withAnnotation;
  }
  
  /**
   * Determines how the program is run and calls the correct findClasses method
   * Supports running via .class and .jar
   * @param packageName - The package to search
   * @return all found classes
   */
  public static List<Class<?>> getClasses(String packageName) throws IOException, ClassNotFoundException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    String resourcePath = packageName.replace(".", "/");  
    Enumeration<URL> resources = cl.getResources(resourcePath);
    List<Class<?>> classes = new ArrayList<>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      String path = resource.getFile().toString();
      if (path.contains("!")) {
        String jarPath = path.substring(0, path.indexOf("!"));
        jarPath = jarPath.replace("file:/", "");
        classes.addAll(findClasses(new JarFile(jarPath), resourcePath));
      } else {
        classes.addAll(findClasses(new File(path), packageName));
      }
    }
    return classes;
  }
  
  /**
   * If the program is run via .jar finds all classes in a package
   * @param packageName - The package to search
   * @return all found classes
   */
  private static List<Class<?>> findClasses(JarFile dir, String packageName) throws ClassNotFoundException {
    List<Class<?>> classes = new ArrayList<>();
    Enumeration<JarEntry> entries = dir.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.contains(packageName) && !entry.isDirectory()) {
        classes.add(Class.forName(name.substring(0, name.length() - 6).replace("/", "."))); // remove .class from filename
      }
    }
    return classes;
  }

  /**
   * If the program is run via .class finds all classes in a package
   * @param packageName - The package to search
   * @return all found classes
   */
  private static List<Class<?>> findClasses(File dir, String packageName) throws ClassNotFoundException {
    List<Class<?>> classes = new ArrayList<>();
    if (!dir.exists()) return classes;
    File[] files = dir.listFiles();
    if (files == null) return classes;
    for (File f : files) {
      if (f.isDirectory()) {
        classes.addAll(findClasses(f, packageName + "." + f.getName()));
      } else if (f.getName().endsWith(".class")) {
        classes.add(Class.forName(packageName + "." + f.getName().substring(0, f.getName().length() - 6))); // remove .class from filename
      }
    }
    return classes;
  }
}
