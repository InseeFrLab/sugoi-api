package fr.insee.sugoi.core.technics.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import fr.insee.sugoi.core.technics.RealmProvider;
import fr.insee.sugoi.core.technics.Store;
import fr.insee.sugoi.core.technics.StoreProvider;
import fr.insee.sugoi.core.technics.StoreStorage;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.UserStorage;

@Component
public class StoreProviderImpl implements StoreProvider {

  @Autowired
  private RealmProvider realmProvider;

  @Autowired
  private StoreStorage storeStorage;

  @Override
  public Store getStoreForUserStorage(String realmName, String userStorageName) {
    Realm r = realmProvider.load(realmName);
    UserStorage us = realmProvider.loadUserStorageByUserStorageName(realmName, userStorageName);
    return storeStorage.getStore(r, us);
  }
}
