package pl.otros.logview.store.async;

import pl.otros.logview.store.LogDataStore;

import java.util.concurrent.Callable;

class OperationClear implements Callable<Integer> {
  private LogDataStore logDataStore;

  public OperationClear(LogDataStore logDataStore) {
    this.logDataStore = logDataStore;
  }

  @Override
  public Integer call() {
     return logDataStore.clear();
  }
}
