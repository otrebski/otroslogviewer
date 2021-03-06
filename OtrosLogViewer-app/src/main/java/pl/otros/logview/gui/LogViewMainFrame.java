/*
 * Copyright 2013 Krzysztof Otrebski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.otros.logview.gui;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.negusoft.singleinstance.SingleInstance;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DataConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXComboBox;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import pl.otros.logview.MarkerColors;
import pl.otros.logview.VersionUtil;
import pl.otros.logview.api.plugins.Plugin;
import pl.otros.logview.batch.BatchProcessor;
import pl.otros.logview.exceptionshandler.EventQueueProxy;
import pl.otros.logview.exceptionshandler.ListUncaughtExceptionHandlers;
import pl.otros.logview.exceptionshandler.LoggingExceptionHandler;
import pl.otros.logview.exceptionshandler.ShowErrorDialogExceptionHandler;
import pl.otros.logview.exceptionshandler.StatusObserverExceptionHandler;
import pl.otros.logview.filter.QueryFilter;
import pl.otros.logview.gui.actions.AboutAction;
import pl.otros.logview.gui.actions.CheckForNewVersionAction;
import pl.otros.logview.gui.actions.ChekForNewVersionOnStartupAction;
import pl.otros.logview.gui.actions.CloseAllTabsAction;
import pl.otros.logview.gui.actions.ConnectToSocketHubAppenderAction;
import pl.otros.logview.gui.actions.ExitAction;
import pl.otros.logview.gui.actions.GettingStartedAction;
import pl.otros.logview.gui.actions.GoToDonatePageAction;
import pl.otros.logview.gui.actions.JumpToMarkedAction;
import pl.otros.logview.gui.actions.JumpToMarkedAction.Direction;
import pl.otros.logview.gui.actions.MarkAllFoundAction;
import pl.otros.logview.gui.actions.OpenLogInvestigationAction;
import pl.otros.logview.gui.actions.OpenPreferencesAction;
import pl.otros.logview.gui.actions.SaveLogInvestigationAction;
import pl.otros.logview.gui.actions.SearchByLevel;
import pl.otros.logview.gui.actions.ShowLoadedPlugins;
import pl.otros.logview.gui.actions.ShowLog4jPatternParserEditor;
import pl.otros.logview.gui.actions.ShowMarkersEditor;
import pl.otros.logview.gui.actions.ShowMessageColorizerEditor;
import pl.otros.logview.gui.actions.ShowOlvLogs;
import pl.otros.logview.gui.actions.StartSocketListener;
import pl.otros.logview.gui.actions.StopAllSocketListeners;
import pl.otros.logview.gui.actions.SwitchAutoJump;
import pl.otros.logview.gui.actions.TailLogActionListener;
import pl.otros.logview.gui.actions.TailLogWithAutoDetectActionListener;
import pl.otros.logview.gui.actions.TailMultipleFilesIntoOneView;
import pl.otros.logview.gui.actions.globalhotkeys.FocusComponentOnHotKey;
import pl.otros.logview.gui.actions.globalhotkeys.KeyboardTabSwitcher;
import pl.otros.logview.gui.actions.read.DragAndDropFilesHandler;
import pl.otros.logview.gui.actions.read.ImportLogWithAutoDetectedImporterActionListener;
import pl.otros.logview.gui.actions.read.ImportLogWithGivenImporterActionListener;
import pl.otros.logview.gui.actions.search.SearchAction;
import pl.otros.logview.gui.actions.search.SearchAction.SearchMode;
import pl.otros.logview.gui.actions.search.SearchDirection;
import pl.otros.logview.gui.actions.search.SearchFieldKeyListener;
import pl.otros.logview.gui.actions.search.SearchModeValidatorDocumentListener;
import pl.otros.logview.gui.message.MessageColorizer;
import pl.otros.logview.gui.message.SearchResultColorizer;
import pl.otros.logview.gui.message.SoapMessageFormatter;
import pl.otros.logview.gui.message.update.MessageUpdateUtils;
import pl.otros.logview.gui.renderers.MarkerColorsComboBoxRenderer;
import pl.otros.logview.gui.services.jumptocode.ServicesImpl;
import pl.otros.logview.gui.tip.TipOfTheDay;
import pl.otros.logview.gui.util.DelayedSwingInvoke;
import pl.otros.logview.gui.util.DocumentInsertUpdateHandler;
import pl.otros.logview.ide.Ide;
import pl.otros.logview.ide.IdeAvailabilityCheck;
import pl.otros.logview.ide.IdeIntegrationConfigAction;
import pl.otros.logview.importer.InitializationException;
import pl.otros.logview.importer.LogImporter;
import pl.otros.logview.loader.IconsLoader;
import pl.otros.logview.loader.LvDynamicLoader;
import pl.otros.logview.logging.GuiJulHandler;
import pl.otros.logview.pluginable.AllPluginables;
import pl.otros.logview.pluginable.PluginableElementEventListener;
import pl.otros.logview.pluginable.PluginableElementsContainer;
import pl.otros.logview.pluginable.PluginablePluginAdapter;
import pl.otros.logview.pluginsimpl.PluginContextImpl;
import pl.otros.logview.reader.SocketLogReader;
import pl.otros.logview.singleinstance.SingleInstanceRequestResponseDelegate;
import pl.otros.swing.config.OtrosConfiguration;
import pl.otros.swing.rulerbar.OtrosJTextWithRulerScrollPane;
import pl.otros.swing.rulerbar.RulerBarHelper;
import pl.otros.vfs.browser.JOtrosVfsBrowserDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static pl.otros.logview.gui.ConfKeys.FORMATTER_SOAP_REMOVE_XSI_FOR_NIL;

public class LogViewMainFrame extends JFrame {
  public static final String VFS_IDENTITIES = "vfs.Identities";
  private static final Logger LOGGER = Logger.getLogger(LogViewMainFrame.class.getName());
  private static final String CARD_LAYOUT_LOGS_TABLE = "cardLayoutLogsTable";
  private static final String CARD_LAYOUT_EMPTY = "cardLayoutEmpty";
  private static SingleInstance singleInstance;
  private JToolBar toolBar;
  private JButton buttonSearch;
  private JLabelStatusObserver observer;
  private JTabbedPane logsTabbedPane;
  private EnableDisableComponetsForTabs enableDisableComponetsForTabs;
  private DataConfiguration configuration;
  private CardLayout cardLayout;
  private JPanel cardLayoutPanel;
  private JXComboBox searchField;
  private AllPluginables allPluginables;
  private PluginableElementsContainer<LogImporter> logImportersContainer;
  private PluginableElementsContainer<MessageColorizer> messageColorizercontainer;
  private SearchResultColorizer searchResultColorizer;
  private OtrosApplication otrosApplication;
  private DefaultComboBoxModel searchFieldCbxModel;
  private ExitAction exitAction;

  public LogViewMainFrame(DataConfiguration c) throws InitializationException {
    super();
    this.configuration = c;
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    String title = "OtrosLogViewer";
    try {
      title += ' ' + VersionUtil.getRunningVersion();
    } catch (Exception e) {
      LOGGER.warning("Can't load version of running OLV");
    }
    this.setTitle(title);
    try {
      String iconPath = "img/otros/logo16.png";
      if (System.getProperty("os.name").contains("Linux")) {
        iconPath = "img/otros/logo64.png";
      }
      BufferedImage icon = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(iconPath));
      this.setIconImage(icon);
    } catch (Exception e1) {
      LOGGER.warning("Can't load icon: " + e1.getMessage());
    }
    Exception modalDisplayException = null;
    // If non-terminal load problem occurs, queue to display for user on
    // top of the app UI.
    try {
      OtrosSplash.setMessage("Loading plugins");
      LvDynamicLoader.getInstance().setStatusObserver(OtrosSplash.getSplashStatusObserver());
      LvDynamicLoader.getInstance().loadAll();
      OtrosSplash.setMessage("Loading plugins loaded");
    } catch (IOException e) {
      LOGGER.severe("Problem with loading automatic markers, filter or log importers: " + e.getMessage());
      modalDisplayException = e;
    } catch (InitializationException ie) {
      // Details should have been logged at lower level
      modalDisplayException = ie;
    }
    OtrosSplash.setMessage("Initializing GUI");
    allPluginables = AllPluginables.getInstance();
    logImportersContainer = allPluginables.getLogImportersContainer();
    messageColorizercontainer = allPluginables.getMessageColorizers();
    searchResultColorizer = (SearchResultColorizer) messageColorizercontainer.getElement(SearchResultColorizer.class.getName());
    cardLayout = new CardLayout();
    cardLayoutPanel = new JPanel(cardLayout);
    JLabel statusLabel = new JLabel(" ");
    observer = new JLabelStatusObserver(statusLabel);
    logsTabbedPane = new JTabbedPane();
    enableDisableComponetsForTabs = new EnableDisableComponetsForTabs(logsTabbedPane);
    logsTabbedPane.addChangeListener(enableDisableComponetsForTabs);

    otrosApplication = new OtrosApplication();
    otrosApplication.setAllPluginables(AllPluginables.getInstance());
    otrosApplication.setApplicationJFrame(this);
    otrosApplication.setConfiguration(configuration);
    otrosApplication.setjTabbedPane(logsTabbedPane);
    otrosApplication.setStatusObserver(observer);
    otrosApplication.setOtrosVfsBrowserDialog(new JOtrosVfsBrowserDialog(getVfsFavoritesConfiguration()));
    otrosApplication.setServices(new ServicesImpl(otrosApplication));
    SingleInstanceRequestResponseDelegate.getInstance().setOtrosApplication(otrosApplication);
    ToolTipManager.sharedInstance().setDismissDelay(5000);

    JProgressBar heapBar = new JProgressBar();
    heapBar.setPreferredSize(new Dimension(190, 15));
    new Thread(new MemoryUsedStatsUpdater(heapBar, 1500), "MemoryUsedUpdater").start();
    JPanel statusPanel = new JPanel(new MigLayout("fill", "[fill, push, grow][right][right]", "[]"));
    statusPanel.add(statusLabel);
    final JButton ideConnectedLabel = new JButton(Ide.IDEA.getIconDiscounted());
    statusPanel.add(ideConnectedLabel);
    statusPanel.add(new JButton(new SwitchAutoJump(otrosApplication)));
    statusPanel.add(heapBar);

    initMenu();
    initToolbar();
    addEmptyViewListener();
    addMenuBarReloadListener();
    otrosApplication.setSearchField(searchField);
    cardLayoutPanel.add(logsTabbedPane, CARD_LAYOUT_LOGS_TABLE);
    EmptyViewPanel emptyViewPanel = new EmptyViewPanel(otrosApplication);
    final JScrollPane jScrollPane = new JScrollPane(emptyViewPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        jScrollPane.getVerticalScrollBar().setValue(0);
      }
    });
    cardLayoutPanel.add(jScrollPane, CARD_LAYOUT_EMPTY);
    cardLayout.show(cardLayoutPanel, CARD_LAYOUT_EMPTY);
    enableDisableComponetsForTabs.stateChanged(null);
    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(toolBar, BorderLayout.NORTH);
    cp.add(cardLayoutPanel, BorderLayout.CENTER);
    cp.add(statusPanel, BorderLayout.SOUTH);
    initGlobalHotKeys();
    initPosition();
    if (configuration.getBoolean(ConfKeys.LOAD_EXPERIMENTAL_FEATURES, false)) {
      initExperimental();
    }
    setTransferHandler(new DragAndDropFilesHandler(otrosApplication));
    initPlugins();
    OtrosSplash.hide();
    setVisible(true);
    if (modalDisplayException != null)
      JOptionPane.showMessageDialog(this,
          "Problem with loading automatic markers,"
              + "filter or log importers:\n"
              + modalDisplayException.getMessage(), "Initialization Error",
          JOptionPane.ERROR_MESSAGE);
    // Check new version on start
    if (c.getBoolean(ConfKeys.VERSION_CHECK_ON_STARTUP, true)) {
      new ChekForNewVersionOnStartupAction(otrosApplication).actionPerformed(null);
    }
    new TipOfTheDay(c).showTipOfTheDayIfNotDisabled(this);
    Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueueProxy());
    ListUncaughtExceptionHandlers listUncaughtExceptionHandlers = new ListUncaughtExceptionHandlers(//
        new LoggingExceptionHandler(),//
        new ShowErrorDialogExceptionHandler(otrosApplication),//
        new StatusObserverExceptionHandler(observer)
    );
    Thread.setDefaultUncaughtExceptionHandler(listUncaughtExceptionHandlers);
    ListeningScheduledExecutorService listeningScheduledExecutorService = otrosApplication.getServices().getTaskSchedulerService().getListeningScheduledExecutorService();
    listeningScheduledExecutorService.scheduleAtFixedRate(
        new IdeAvailabilityCheck(ideConnectedLabel, otrosApplication.getServices().getJumpToCodeService()),
        5, 5, TimeUnit.SECONDS);
    ideConnectedLabel.addActionListener(new IdeIntegrationConfigAction(otrosApplication));
  }

  /**
   * @param args porgram CLI arguments
   * @throws InitializationException
   * @throws InvocationTargetException
   * @throws InterruptedException
   */
  public static void main(final String[] args) throws InitializationException, InterruptedException, InvocationTargetException {
    if (args.length > 0 && "-batch".equals(args[0])) {
      try {
        String[] batchArgs = new String[args.length - 1];
        System.arraycopy(args, 1, batchArgs, 0, batchArgs.length);
        BatchProcessor.main(batchArgs);
      } catch (IOException e) {
        System.err.println("Error during batch processing: " + e.getMessage());
        e.printStackTrace();
      } catch (ConfigurationException e) {
        System.err.println("Error during batch processing: " + e.getMessage());
        e.printStackTrace();
      }
      return;
    }
    SingleInstanceRequestResponseDelegate singleInstanceRequestResponseDelegate = SingleInstanceRequestResponseDelegate.getInstance();
    singleInstance = SingleInstance.request("OtrosLogViewer", singleInstanceRequestResponseDelegate,
        singleInstanceRequestResponseDelegate, args);
    if (singleInstance == null) {
      LOGGER.info("OtrosLogViewer is already running, params send using requestAction");
      System.exit(0);
    }
    GuiJulHandler handler = new GuiJulHandler();
    handler.setLevel(Level.ALL);
    Logger olvLogger = Logger.getLogger("pl.otros.logview");
    olvLogger.setLevel(Level.ALL);
    olvLogger.addHandler(handler);
    LOGGER.info("Starting application");
    OtrosSplash.setMessage("Starting application");
    OtrosSplash.setMessage("Loading configuration");
    final XMLConfiguration c = getConfiguration("config.xml");
    if (!c.containsKey(ConfKeys.UUID)) {
      c.setProperty(ConfKeys.UUID, UUID.randomUUID().toString());
    }
    IconsLoader.loadIcons();
    OtrosSplash.setMessage("Loading icons");
    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        try {
          OtrosSplash.setMessage("Loading L&F");
          String lookAndFeel = c.getString("lookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
          LOGGER.config("Initializing look and feelL: " + lookAndFeel);
          PlasticLookAndFeel.setTabStyle(Plastic3DLookAndFeel.TAB_STYLE_METAL_VALUE);
          UIManager.setLookAndFeel(lookAndFeel);
        } catch (Throwable e1) {
          LOGGER.warning("Cannot initialize LookAndFeel: " + e1.getMessage());
        }
        try {
          final DataConfiguration c1 = new OtrosConfiguration(c);
          final LogViewMainFrame mf = new LogViewMainFrame(c1);
          // mf.exitAction was instantiated in the constructor (previous line)
          // Not sure retrieving this from most appropriate Apache config
          // object.
          mf.exitAction.setConfirm(
              c.getBoolean("generalBehavior.confirmExit", true));
          /* TODO:  Implement User Preferences screen or checkbox on exit widget
           * that will update the same config object something like:
           *     c.setProperty("generalBehavior.confirmExit", newValue);
           */
          mf.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
              c.setProperty("gui.state", mf.getExtendedState());
              if (mf.getExtendedState() == Frame.NORMAL) {
                c.setProperty("gui.width", mf.getWidth());
                c.setProperty("gui.height", mf.getHeight());
              }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
              c.setProperty("gui.location.x", mf.getLocation().x);
              c.setProperty("gui.location.y", mf.getLocation().y);
            }
          });
          mf.addWindowListener(mf.exitAction);
          SingleInstanceRequestResponseDelegate.openFilesFromStartArgs(mf.otrosApplication, Arrays.asList(args),
              mf.otrosApplication.getAppProperties().getCurrentDir());
        } catch (InitializationException e) {
          LOGGER.log(Level.SEVERE, "Cannot initialize main frame", e);
        }
      }
    });
  }

  private static XMLConfiguration getConfiguration(String file) {
    XMLConfiguration commonConfiguration = new XMLConfiguration();
    File commonConfigurationFile = new File(file);
    // load common configuration
    if (commonConfigurationFile.exists()) {
      LOGGER.info("Loading common configuration from " + commonConfigurationFile.getAbsolutePath());
      try {
        commonConfiguration.load(commonConfigurationFile);
      } catch (ConfigurationException e) {
        LOGGER.severe("Can't load configuration, creating new " + e.getMessage());
      }
    } else {
      LOGGER.info("Common configuration file do not exist");
    }
    // load user specific configuration
    if (!AllPluginables.USER_CONFIGURATION_DIRECTORY.exists()) {
      LOGGER.info("Creating user specific OtrosLogViewer configuration directory " + AllPluginables.USER_CONFIGURATION_DIRECTORY.getAbsolutePath());
      AllPluginables.USER_CONFIGURATION_DIRECTORY.mkdirs();
      AllPluginables.USER_FILTER.mkdirs();
      AllPluginables.USER_LOG_IMPORTERS.mkdirs();
      AllPluginables.USER_MARKERS.mkdirs();
      AllPluginables.USER_MESSAGE_FORMATTER_COLORZIERS.mkdirs();
    }
    XMLConfiguration userConfiguration = new XMLConfiguration();
    File userConfigurationFile = new File(AllPluginables.USER_CONFIGURATION_DIRECTORY + File.separator + file);
    userConfiguration.setFile(userConfigurationFile);
    if (userConfigurationFile.exists()) {
      try {
        userConfiguration.load();
      } catch (ConfigurationException e) {
        LOGGER.severe(String.format("Can't load user configuration from %s: %s", userConfigurationFile.getAbsolutePath(), e.getMessage()));
      }
    }
    Iterator<?> keys = commonConfiguration.getKeys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      if (!userConfiguration.containsKey(key)) {
        userConfiguration.setProperty(key, commonConfiguration.getProperty(key));
      }
    }
    userConfiguration.setAutoSave(true);
    return userConfiguration;
  }

  private static Configuration getVfsFavoritesConfiguration() {
    File file = new File(AllPluginables.USER_CONFIGURATION_DIRECTORY + File.separator + "vfsFavorites.xml");
    XMLConfiguration configuration = new XMLConfiguration();
    configuration.setFile(file);
    if (file.exists()) {
      try {
        configuration.load();
      } catch (ConfigurationException e) {
        LOGGER.severe(String.format("Can't load user configuration from %s: %s", file.getAbsolutePath(), e.getMessage()));
      }
    }
    configuration.setAutoSave(true);
    return configuration;
  }

  private void initPlugins() {
    LOGGER.info("Loading plugins");
    PluginContextImpl contextImpl = new PluginContextImpl(otrosApplication);
    PluginableElementsContainer<PluginablePluginAdapter> pluginsInfoContainer = AllPluginables.getInstance().getPluginsInfoContainer();
    for (PluginablePluginAdapter ppi : pluginsInfoContainer.getElements()) {
      try {
        Plugin plugin = ppi.getPlugin();
        plugin.initialize(contextImpl);
      } catch (Exception e) {
        LOGGER.severe("Can't initialize plugins " + ppi.getName() + "\n" + Throwables.getStackTraceAsString(e));
      }
    }
  }

  private void addEmptyViewListener() {
    logsTabbedPane.addContainerListener(new ContainerListener() {
      @Override
      public void componentRemoved(ContainerEvent e) {
        setCard();
      }

      @Override
      public void componentAdded(ContainerEvent e) {
        setCard();
      }

      public void setCard() {
        int tabCount = logsTabbedPane.getTabCount();
        if (tabCount > 0) {
          cardLayout.show(cardLayoutPanel, CARD_LAYOUT_LOGS_TABLE);
        } else {
          cardLayout.show(cardLayoutPanel, CARD_LAYOUT_EMPTY);
        }
      }
    });
  }

  private void addMenuBarReloadListener() {
    PluginableElementEventListener<LogImporter> l = new PluginableElementEventListener<LogImporter>() {
      @Override
      public void elementAdded(LogImporter element) {
        reloadMenuBar();
      }

      @Override
      public void elementRemoved(LogImporter element) {
        reloadMenuBar();
      }

      @Override
      public void elementChanged(LogImporter element) {
        reloadMenuBar();
      }

      private void reloadMenuBar() {
        GuiUtils.runLaterInEdt(new Runnable() {
          @Override
          public void run() {
            initMenu();
            //without validating tree menu bar is inactive
            synchronized (LogViewMainFrame.this.getTreeLock()) {
              LogViewMainFrame.this.validateTree();
            }
          }
        });
      }
    };
    otrosApplication.getAllPluginables().getLogImportersContainer().addListener(l);
  }

  private void initToolbar() {
    toolBar = new JToolBar();
    final JComboBox searchMode = new JComboBox(new String[]{"String contains search: ", "Regex search: ", "Query search: "});
    final SearchAction searchActionForward = new SearchAction(otrosApplication, SearchDirection.FORWARD);
    final SearchAction searchActionBackward = new SearchAction(otrosApplication, SearchDirection.REVERSE);
    searchFieldCbxModel = new DefaultComboBoxModel();
    searchField = new JXComboBox(searchFieldCbxModel);
    searchField.setEditable(true);
    AutoCompleteDecorator.decorate(searchField);
    searchField.setMinimumSize(new Dimension(150, 10));
    searchField.setPreferredSize(new Dimension(250, 10));
    searchField.setToolTipText("<HTML>Enter text to search.<BR/>" + "Enter - search next,<BR/>Alt+Enter search previous,<BR/>"
        + "Ctrl+Enter - mark all found</HTML>");
    final DelayedSwingInvoke delayedSearchResultUpdate = new DelayedSwingInvoke() {
      @Override
      protected void performActionHook() {
        JTextComponent editorComponent = (JTextComponent) searchField.getEditor().getEditorComponent();
        int stringEnd = editorComponent.getSelectionStart();
        if (stringEnd < 0) {
          stringEnd = editorComponent.getText().length();
        }
        try {
          String selectedText = editorComponent.getText(0, stringEnd);
          if (StringUtils.isBlank(selectedText)) {
            return;
          }
          OtrosJTextWithRulerScrollPane<JTextPane> logDetailWithRulerScrollPane = otrosApplication.getSelectedLogViewPanel().getLogDetailWithRulerScrollPane();
          MessageUpdateUtils.highlightSearchResult(logDetailWithRulerScrollPane, otrosApplication.getAllPluginables().getMessageColorizers());
          RulerBarHelper.scrollToFirstMarker(logDetailWithRulerScrollPane);
        } catch (BadLocationException e) {
          LOGGER.log(Level.SEVERE, "Can't update search highlight", e);
        }
      }
    };
    JTextComponent searchFieldTextComponent = (JTextComponent) searchField.getEditor().getEditorComponent();
    searchFieldTextComponent.getDocument().addDocumentListener(new DocumentInsertUpdateHandler() {
      @Override
      protected void documentChanged(DocumentEvent e) {
        delayedSearchResultUpdate.performAction();
      }
    });
    final MarkAllFoundAction markAllFoundAction = new MarkAllFoundAction(otrosApplication);
    final SearchModeValidatorDocumentListener searchValidatorDocumentListener = new SearchModeValidatorDocumentListener((JTextField) searchField.getEditor().getEditorComponent(), observer,
        SearchMode.STRING_CONTAINS);
    SearchMode searchModeFromConfig = configuration.get(SearchMode.class, "gui.searchMode", SearchMode.STRING_CONTAINS);
    final String lastSearchString;
    int selectedSearchMode = 0;
    if (searchModeFromConfig.equals(SearchMode.STRING_CONTAINS)) {
      selectedSearchMode = 0;
      lastSearchString = configuration.getString(ConfKeys.SEARCH_LAST_STRING, "");
    } else if (searchModeFromConfig.equals(SearchMode.REGEX)) {
      selectedSearchMode = 1;
      lastSearchString = configuration.getString(ConfKeys.SEARCH_LAST_REGEX, "");
    } else if (searchModeFromConfig.equals(SearchMode.QUERY)) {
      selectedSearchMode = 2;
      lastSearchString = configuration.getString(ConfKeys.SEARCH_LAST_QUERY, "");
    } else {
      LOGGER.warning("Unknown search mode " + searchModeFromConfig);
      lastSearchString = "";
    }
    Component editorComponent = searchField.getEditor().getEditorComponent();
    if (editorComponent instanceof JTextField) {
      final JTextField sfTf = (JTextField) editorComponent;
      sfTf.getDocument().addDocumentListener(searchValidatorDocumentListener);
      sfTf.getDocument().addDocumentListener(new DocumentInsertUpdateHandler() {
        @Override
        protected void documentChanged(DocumentEvent e) {
          try {
            int length = e.getDocument().getLength();
            if (length > 0) {
              searchResultColorizer.setSearchString(e.getDocument().getText(0, length));
            }
          } catch (BadLocationException e1) {
            LOGGER.log(Level.SEVERE, "Error: ", e1);
          }
        }
      });
      sfTf.addKeyListener(new SearchFieldKeyListener(searchActionForward, sfTf));
      sfTf.setText(lastSearchString);
    }
    searchMode.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SearchMode mode = null;
        boolean validationEnabled = false;
        String confKey = null;
        String lastSearch = ((JTextField) searchField.getEditor().getEditorComponent()).getText();
        if (searchMode.getSelectedIndex() == 0) {
          mode = SearchMode.STRING_CONTAINS;
          searchValidatorDocumentListener.setSearchMode(mode);
          validationEnabled = false;
          searchMode.setToolTipText("Checking if log message contains string (case is ignored)");
          confKey = ConfKeys.SEARCH_LAST_STRING;
        } else if (searchMode.getSelectedIndex() == 1) {
          mode = SearchMode.REGEX;
          validationEnabled = true;
          searchMode.setToolTipText("Checking if log message matches regular expression (case is ignored)");
          confKey = ConfKeys.SEARCH_LAST_REGEX;
        } else if (searchMode.getSelectedIndex() == 2) {
          mode = SearchMode.QUERY;
          validationEnabled = true;
          String querySearchTooltip = "<HTML>" + //
              "Advance search using SQL-like quries (i.e. level>=warning && msg~=failed && thread==t1)<BR/>" + //
              "Valid operator for query search is ==, ~=, !=, LIKE, EXISTS, <, <=, >, >=, &&, ||, ! <BR/>" + //
              "See wiki for more info<BR/>" + //
              "</HTML>";
          searchMode.setToolTipText(querySearchTooltip);
          confKey = ConfKeys.SEARCH_LAST_QUERY;
        }
        searchValidatorDocumentListener.setSearchMode(mode);
        searchValidatorDocumentListener.setEnable(validationEnabled);
        searchActionForward.setSearchMode(mode);
        searchActionBackward.setSearchMode(mode);
        markAllFoundAction.setSearchMode(mode);
        configuration.setProperty("gui.searchMode", mode);
        searchResultColorizer.setSearchMode(mode);
        List<Object> list = configuration.getList(confKey);
        searchFieldCbxModel.removeAllElements();
        for (Object o : list) {
          searchFieldCbxModel.addElement(o);
        }
        searchField.setSelectedItem(lastSearch);
      }
    });
    searchMode.setSelectedIndex(selectedSearchMode);
    final JCheckBox markFound = new JCheckBox("Mark search result");
    markFound.setMnemonic(KeyEvent.VK_M);
    searchField.addKeyListener(markAllFoundAction);
    configuration.addConfigurationListener(markAllFoundAction);
    JButton markAllFoundButton = new JButton(markAllFoundAction);
    final JComboBox markColor = new JComboBox(MarkerColors.values());
    markFound.setSelected(configuration.getBoolean("gui.markFound", true));
    markFound.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        boolean selected = markFound.isSelected();
        searchActionForward.setMarkFound(selected);
        searchActionBackward.setMarkFound(selected);
        configuration.setProperty("gui.markFound", markFound.isSelected());
      }
    });
    markColor.setRenderer(new MarkerColorsComboBoxRenderer());
//		markColor.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				MarkerColors markerColors = (MarkerColors) markColor.getSelectedItem();
//				searchActionForward.setMarkerColors(markerColors);
//				searchActionBackward.setMarkerColors(markerColors);
//				markAllFoundAction.setMarkerColors(markerColors);
//				configuration.setProperty("gui.markColor", markColor.getSelectedItem());
//				otrosApplication.setSelectedMarkColors(markerColors);
//			}
//		});
    markColor.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        MarkerColors markerColors = (MarkerColors) markColor.getSelectedItem();
        searchActionForward.setMarkerColors(markerColors);
        searchActionBackward.setMarkerColors(markerColors);
        markAllFoundAction.setMarkerColors(markerColors);
        configuration.setProperty("gui.markColor", markColor.getSelectedItem());
        otrosApplication.setSelectedMarkColors(markerColors);
      }
    });
    markColor.getModel().setSelectedItem(configuration.get(MarkerColors.class, "gui.markColor", MarkerColors.Aqua));
    buttonSearch = new JButton(searchActionForward);
    buttonSearch.setMnemonic(KeyEvent.VK_N);
    JButton buttonSearchPrev = new JButton(searchActionBackward);
    buttonSearchPrev.setMnemonic(KeyEvent.VK_P);
    enableDisableComponetsForTabs.addComponet(buttonSearch);
    enableDisableComponetsForTabs.addComponet(buttonSearchPrev);
    enableDisableComponetsForTabs.addComponet(searchField);
    enableDisableComponetsForTabs.addComponet(markFound);
    enableDisableComponetsForTabs.addComponet(markAllFoundButton);
    enableDisableComponetsForTabs.addComponet(searchMode);
    enableDisableComponetsForTabs.addComponet(markColor);
    toolBar.add(searchMode);
    toolBar.add(searchField);
    toolBar.add(buttonSearch);
    toolBar.add(buttonSearchPrev);
    toolBar.add(markFound);
    toolBar.add(markAllFoundButton);
    toolBar.add(markColor);
    JButton nextMarked = new JButton(new JumpToMarkedAction(otrosApplication, Direction.FORWARD));
    nextMarked.setToolTipText(nextMarked.getText());
    nextMarked.setText("");
    nextMarked.setMnemonic(KeyEvent.VK_E);
    enableDisableComponetsForTabs.addComponet(nextMarked);
    toolBar.add(nextMarked);
    JButton prevMarked = new JButton(new JumpToMarkedAction(otrosApplication, Direction.BACKWARD));
    prevMarked.setToolTipText(prevMarked.getText());
    prevMarked.setText("");
    prevMarked.setMnemonic(KeyEvent.VK_R);
    enableDisableComponetsForTabs.addComponet(prevMarked);
    toolBar.add(prevMarked);
    enableDisableComponetsForTabs.addComponet(toolBar.add(new SearchByLevel(otrosApplication, 1, Level.INFO)));
    enableDisableComponetsForTabs.addComponet(toolBar.add(new SearchByLevel(otrosApplication, 1, Level.WARNING)));
    enableDisableComponetsForTabs.addComponet(toolBar.add(new SearchByLevel(otrosApplication, 1, Level.SEVERE)));
    enableDisableComponetsForTabs.addComponet(toolBar.add(new SearchByLevel(otrosApplication, -1, Level.INFO)));
    enableDisableComponetsForTabs.addComponet(toolBar.add(new SearchByLevel(otrosApplication, -1, Level.WARNING)));
    enableDisableComponetsForTabs.addComponet(toolBar.add(new SearchByLevel(otrosApplication, -1, Level.SEVERE)));
  }

  private void initMenu() {
    JMenuBar menuBar = getJMenuBar();
    if (menuBar == null) {
      menuBar = new JMenuBar();
      setJMenuBar(menuBar);
    }
    menuBar.removeAll();
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    JLabel labelOpenLog = new JLabel("Open log", Icons.FOLDER_OPEN, SwingConstants.LEFT);
    Font menuGroupFont = labelOpenLog.getFont().deriveFont(13f).deriveFont(Font.BOLD);
    labelOpenLog.setFont(menuGroupFont);
    fileMenu.add(labelOpenLog);
    JMenuItem openAutoDetectLog = new JMenuItem("Open log with autodetect type");
    openAutoDetectLog.addActionListener(new ImportLogWithAutoDetectedImporterActionListener(otrosApplication));
    openAutoDetectLog.setMnemonic(KeyEvent.VK_O);
    openAutoDetectLog.setIcon(Icons.WIZARD);
    fileMenu.add(openAutoDetectLog);
    JMenuItem tailAutoDetectLog = new JMenuItem("Tail log with autodetect type");
    tailAutoDetectLog.addActionListener(new TailLogWithAutoDetectActionListener(otrosApplication));
    tailAutoDetectLog.setMnemonic(KeyEvent.VK_T);
    tailAutoDetectLog.setIcon(Icons.ARROW_REPEAT);
    fileMenu.add(tailAutoDetectLog);
    fileMenu.add(new TailMultipleFilesIntoOneView(otrosApplication));
    fileMenu.add(new ConnectToSocketHubAppenderAction(otrosApplication));
    fileMenu.add(new JSeparator());
    JLabel labelLogInvestigation = new JLabel("Log investigation", SwingConstants.LEFT);
    labelLogInvestigation.setFont(menuGroupFont);
    fileMenu.add(labelLogInvestigation);
    fileMenu.add(new OpenLogInvestigationAction(otrosApplication));
    JMenuItem saveLogsInvest = new JMenuItem(new SaveLogInvestigationAction(otrosApplication));
    enableDisableComponetsForTabs.addComponet(saveLogsInvest);
    fileMenu.add(saveLogsInvest);
    fileMenu.add(new JSeparator());
    LogImporter[] importers = new LogImporter[0];
    importers = logImportersContainer.getElements().toArray(importers);
    for (LogImporter logImporter : importers) {
      JMenuItem openLog = new JMenuItem("Open " + logImporter.getName() + " log");
      openLog.addActionListener(new ImportLogWithGivenImporterActionListener(otrosApplication, logImporter));
      if (logImporter.getKeyStrokeAccelelator() != null) {
        openLog.setAccelerator(KeyStroke.getKeyStroke(logImporter.getKeyStrokeAccelelator()));
      }
      if (logImporter.getMnemonic() > 0) {
        openLog.setMnemonic(logImporter.getMnemonic());
      }
      Icon icon = logImporter.getIcon();
      if (icon != null) {
        openLog.setIcon(icon);
      }
      fileMenu.add(openLog);
    }
    fileMenu.add(new JSeparator());
    JLabel labelTailLog = new JLabel("Tail log [from begging of file]", Icons.ARROW_REPEAT, SwingConstants.LEFT);
    labelTailLog.setFont(menuGroupFont);
    fileMenu.add(labelTailLog);
    for (LogImporter logImporter : importers) {
      JMenuItem openLog = new JMenuItem("Tail " + logImporter.getName() + " log");
      openLog.addActionListener(new TailLogActionListener(otrosApplication, logImporter));
      if (logImporter.getKeyStrokeAccelelator() != null) {
        openLog.setAccelerator(KeyStroke.getKeyStroke(logImporter.getKeyStrokeAccelelator()));
      }
      if (logImporter.getMnemonic() > 0) {
        openLog.setMnemonic(logImporter.getMnemonic());
      }
      Icon icon = logImporter.getIcon();
      if (icon != null) {
        openLog.setIcon(icon);
      }
      fileMenu.add(openLog);
    }
    JMenuItem exitMenuItem = new JMenuItem("Exit", 'e');
    exitMenuItem.setIcon(Icons.TURN_OFF);
    exitMenuItem.setAccelerator(KeyStroke.getKeyStroke("control F4"));
    exitAction = new ExitAction(this);
    exitMenuItem.addActionListener(exitAction);
    fileMenu.add(new JSeparator());
    fileMenu.add(exitMenuItem);
    JMenu toolsMenu = new JMenu("Tools");
    toolsMenu.setMnemonic(KeyEvent.VK_T);
    JMenuItem closeAll = new JMenuItem(new CloseAllTabsAction(otrosApplication));
    enableDisableComponetsForTabs.addComponet(closeAll);
    ArrayList<SocketLogReader> logReaders = new ArrayList<SocketLogReader>();
    toolsMenu.add(new JMenuItem(new StartSocketListener(otrosApplication, logReaders)));
    toolsMenu.add(new JMenuItem(new StopAllSocketListeners(otrosApplication, logReaders)));
    toolsMenu.add(new ShowMarkersEditor(otrosApplication));
    toolsMenu.add(new ShowLog4jPatternParserEditor(otrosApplication));
    toolsMenu.add(new ShowMessageColorizerEditor(otrosApplication));
    toolsMenu.add(new ShowLoadedPlugins(otrosApplication));
    toolsMenu.add(new ShowOlvLogs(otrosApplication));
    toolsMenu.add(new OpenPreferencesAction(otrosApplication));
    toolsMenu.add(closeAll);
    JMenu pluginsMenu = new JMenu("Plugins");
    otrosApplication.setPluginsMenu(pluginsMenu);
    JMenu helpMenu = new JMenu("Help");
    JMenuItem about = new JMenuItem("About");
    AboutAction action = new AboutAction(otrosApplication);
    action.putValue(Action.NAME, "About");
    about.setAction(action);
    helpMenu.add(about);
    helpMenu.add(new GoToDonatePageAction(otrosApplication));
    JMenuItem checkForNewVersion = new JMenuItem(new CheckForNewVersionAction(otrosApplication));
    helpMenu.add(checkForNewVersion);
    helpMenu.add(new GettingStartedAction(otrosApplication));
    menuBar.add(fileMenu);
    menuBar.add(toolsMenu);
    menuBar.add(pluginsMenu);
    menuBar.add(helpMenu);
  }

  private void initExperimental() {
    JMenu menu = new JMenu("Experimental");
    menu.add(new JLabel("Experimental features, can have bugs", Icons.LEVEL_WARNING, SwingConstants.LEADING));
    menu.add(new JSeparator());
    boolean storeOnDisk = StringUtils.equalsIgnoreCase(System.getProperty("cacheEvents"), "true");
    JRadioButtonMenuItem radioButtonMemory = new JRadioButtonMenuItem("Memory - faster, more memory required", !storeOnDisk);
    radioButtonMemory.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.setProperty("cacheEvents", Boolean.FALSE.toString());
      }
    });
    JRadioButtonMenuItem radioButtonDisk = new JRadioButtonMenuItem("Disk with caching - slower, less memory required", storeOnDisk);
    radioButtonDisk.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.setProperty("cacheEvents", Boolean.TRUE.toString());
      }
    });
    final ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(radioButtonDisk);
    buttonGroup.add(radioButtonMemory);
    menu.add(new JSeparator(JSeparator.VERTICAL));
    menu.add(new JLabel("Keep parsed log events store:"));
    menu.add(radioButtonMemory);
    menu.add(radioButtonDisk);
    final JCheckBox soapFormatterRemoveMultirefsCbx = new JCheckBox();
    soapFormatterRemoveMultirefsCbx.setSelected(configuration.getBoolean(ConfKeys.FORMATTER_SOAP_REMOVE_MULTIREFS, false));
    AbstractAction enableMultiRefRemoveFeature = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SoapMessageFormatter soapMessageFormatter = (SoapMessageFormatter) AllPluginables.getInstance().getMessageFormatters().getElement(SoapMessageFormatter.class.getName());
        soapMessageFormatter.setRemoveMultiRefs(soapFormatterRemoveMultirefsCbx.isSelected());
        configuration.setProperty(ConfKeys.FORMATTER_SOAP_REMOVE_MULTIREFS, soapFormatterRemoveMultirefsCbx.isSelected());
      }
    };
    enableMultiRefRemoveFeature.putValue(Action.NAME, "Remove mulitRefs from SOAP messages");
    soapFormatterRemoveMultirefsCbx.setAction(enableMultiRefRemoveFeature);
    enableMultiRefRemoveFeature.actionPerformed(null);
    final JCheckBox soapFormatterRemoveXsiForNilElementsCbx = new JCheckBox();
    soapFormatterRemoveXsiForNilElementsCbx.setSelected(configuration.getBoolean(FORMATTER_SOAP_REMOVE_XSI_FOR_NIL, false));
    AbstractAction soapFormatterRemoveXsiFromNilAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SoapMessageFormatter soapMessageFormatter = (SoapMessageFormatter) AllPluginables.getInstance().getMessageFormatters().getElement(SoapMessageFormatter.class.getName());
        soapMessageFormatter.setRemoveXsiForNilElements(soapFormatterRemoveXsiForNilElementsCbx.isSelected());
        configuration.setProperty(FORMATTER_SOAP_REMOVE_XSI_FOR_NIL, soapFormatterRemoveXsiForNilElementsCbx.isSelected());
      }
    };
    soapFormatterRemoveXsiFromNilAction.putValue(Action.NAME, "Remove xsi for for NIL elements from SOAP messages");
    soapFormatterRemoveXsiForNilElementsCbx.setAction(soapFormatterRemoveXsiFromNilAction);
    soapFormatterRemoveXsiFromNilAction.actionPerformed(null);
    menu.add(soapFormatterRemoveMultirefsCbx);
    menu.add(soapFormatterRemoveXsiForNilElementsCbx);
    getJMenuBar().add(menu);
    QueryFilter queryFilter = new QueryFilter();
    allPluginables.getLogFiltersContainer().addElement(queryFilter);
    JButton b = new JButton("Throw exception");
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (System.currentTimeMillis() % 2 == 0) {
          throw new RuntimeException("Exception swing action!");
        } else {
          new Thread(new Runnable() {
            @Override
            public void run() {
              throw new RuntimeException("Exception from tread!");
            }
          }).start();
        }
      }
    });
    menu.add(b);
  }

  private void initGlobalHotKeys() {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventPostProcessor(new KeyboardTabSwitcher(logsTabbedPane));
    manager.addKeyEventPostProcessor(new FocusComponentOnHotKey(searchField, KeyEvent.VK_F, KeyEvent.META_MASK));
  }

  private void initPosition() {
    Dimension size = new Dimension(configuration.getInt("gui.width", 1280), configuration.getInt("gui.height", 780));
    Point location = new Point(configuration.getInt("gui.location.x", 100), configuration.getInt("gui.location.y", 100));
    int state = configuration.getInt("gui.state", Frame.NORMAL);
    Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
    if (location.x > screensize.width) {
      location.x = 0;
    }
    if (location.y > screensize.height) {
      location.y = 0;
    }
    size.width = Math.min(screensize.width, size.width);
    size.height = Math.min(screensize.height, size.height);
    this.setSize(size);
    this.setLocation(location);
    this.setExtendedState(state);
  }
}
