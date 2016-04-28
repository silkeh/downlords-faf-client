package com.faforever.client.fx;

import com.faforever.client.theme.ThemeService;
import javafx.fxml.FXMLLoader;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Locale;

public class FxmlLoaderImpl implements FxmlLoader {

  private final ThreadLocal<FXMLLoader> threadLocalFXMLLoader;


  @Resource
  MessageSource messageSource;
  @Resource
  Locale locale;
  @Resource
  ThemeService themeService;

  private MessageSourceResourceBundle resources;

  public FxmlLoaderImpl() {
    threadLocalFXMLLoader = new ThreadLocal<FXMLLoader>() {
      @Override
      protected FXMLLoader initialValue() {
        return new FXMLLoader();
      }
    };
  }

  @PostConstruct
  void postConstruct() {
    resources = new MessageSourceResourceBundle(messageSource, locale);
  }

  @Override
  public <T> T loadAndGetController(String file) {
    return load(file, null, null).getController();
  }

  @Override
  public void loadCustomControl(String file, Object control) {
    load(file, control, control);
  }

  @Override
  public <T> T loadAndGetRoot(String file) {
    return loadAndGetRoot(file, null);
  }

  @Override
  public <T> T loadAndGetRoot(String file, Object controller) {
    return load(file, controller, null).getRoot();
  }

  private FXMLLoader load(String file, Object controller, Object root) {
    try {
      FXMLLoader loader = this.threadLocalFXMLLoader.get();
      loader.setController(controller);
      loader.setRoot(root);
      loader.setLocation(themeService.getThemeFileUrl(file));
      loader.setResources(resources);
      loader.load();
      return loader;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
