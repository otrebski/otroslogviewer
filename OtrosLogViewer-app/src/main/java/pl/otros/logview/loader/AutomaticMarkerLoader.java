/*******************************************************************************
 * Copyright 2011 Krzysztof Otrebski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package pl.otros.logview.loader;

import com.google.common.base.Splitter;
import pl.otros.logview.gui.markers.AutomaticMarker;
import pl.otros.logview.gui.markers.PropertyFileAbstractMarker;
import pl.otros.logview.gui.markers.RegexMarker;
import pl.otros.logview.gui.markers.StringMarker;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutomaticMarkerLoader {

  private static final Logger LOGGER = Logger.getLogger(AutomaticMarker.class.getName());
  private static BaseLoader baseLoader = new BaseLoader();

  public static ArrayList<AutomaticMarker> loadInternalMarkers() throws IOException {


    ArrayList<AutomaticMarker> markers = new ArrayList<AutomaticMarker>();
    Properties p = new Properties();
    p.load(AutomaticMarkerLoader.class.getClassLoader().getResourceAsStream("markers.properties"));
    final Iterable<String> defaultMarkers = Splitter.on(',').split(p.getProperty("defaultMarkers"));
    for (String line : defaultMarkers) {
      try {
        Class<?> c = AutomaticMarkerLoader.class.getClassLoader().loadClass(line);
        AutomaticMarker am = (AutomaticMarker) c.newInstance();
        markers.add(am);
      } catch (ClassNotFoundException e) {
        LOGGER.log(Level.SEVERE, "Error loading class " + line, e);
      } catch (InstantiationException e) {
        LOGGER.log(Level.SEVERE, "Error loading class " + line, e);
      } catch (IllegalAccessException e) {
        LOGGER.log(Level.SEVERE, "Error loading class " + line, e);
      }
    }

    return markers;

  }

  public static ArrayList<AutomaticMarker> load(File dir) {
    ArrayList<AutomaticMarker> markers = new ArrayList<AutomaticMarker>();
    markers.addAll(baseLoader.load(dir, AutomaticMarker.class));
    return markers;
  }

  public static ArrayList<AutomaticMarker> loadRegexMarkers(File dir) {
    ArrayList<AutomaticMarker> markers = new ArrayList<AutomaticMarker>();
    File[] files = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isFile() && pathname.getName().endsWith(".regexMarker");
      }
    });
    if (files != null) {
      for (File file : files) {
        try {
          markers.add(loadRegexMarkerFromProperties(file));
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Cannot initialize RegexMarker from file " + file.getName(), e);
        }
      }
    }
    return markers;
  }

  public static ArrayList<AutomaticMarker> loadStringMarkers(File dir) {
    ArrayList<AutomaticMarker> markers = new ArrayList<AutomaticMarker>();
    File[] files = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isFile() && pathname.getName().endsWith(".stringMarker");
      }
    });
    if (files != null) {
      for (File file : files) {
        try {
          markers.add(loadStringMarkerFromProperties(file));
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Cannot initialize StringMarker from file " + file.getName(), e);
        }
      }
    }
    return markers;
  }

  private static AutomaticMarker loadRegexMarkerFromProperties(File file) throws Exception {
    Properties p = new Properties();
    p.load(new FileInputStream(file));
    RegexMarker marker = new RegexMarker(p);
    marker.setFileName(file.getName());
    return marker;
  }

  private static AutomaticMarker loadStringMarkerFromProperties(File file) throws Exception {
    Properties p = new Properties();
    p.load(new FileInputStream(file));
    StringMarker marker = new StringMarker(p);
    marker.setFileName(file.getName());
    return marker;
  }

  public static AutomaticMarker loadPropertyBasedMarker(Properties p) throws Exception {
    String type = p.getProperty(PropertyFileAbstractMarker.TYPE, "");
    AutomaticMarker marker = null;
    if (type.equalsIgnoreCase(PropertyFileAbstractMarker.TYPE_STRING)) {
      marker = new StringMarker(p);
    } else if (type.equalsIgnoreCase(PropertyFileAbstractMarker.TYPE_REGEX)) {
      marker = new RegexMarker(p);
    }
    if (marker == null) {
      throw new Exception("Unknown type of marker: " + type);
    }
    return marker;
  }

  public static Collection<? extends AutomaticMarker> loadPatternMarker(File dir) {
    ArrayList<AutomaticMarker> markers = new ArrayList<AutomaticMarker>();
    File[] files = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isFile() && pathname.getName().endsWith(".marker");
      }
    });
    if (files != null) {
      for (File file : files) {
        try {
          Properties p = new Properties();
          FileInputStream fin = new FileInputStream(file);
          p.load(fin);
          markers.add(loadPropertyBasedMarker(p));
          fin.close();
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Cannot initialize RegexMarker from file " + file.getName(), e);
        }
      }
    }
    return markers;
  }

}
