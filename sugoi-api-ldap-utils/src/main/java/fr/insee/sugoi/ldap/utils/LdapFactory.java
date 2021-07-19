/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fr.insee.sugoi.ldap.utils;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import fr.insee.sugoi.ldap.utils.config.LdapConfigKeys;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LdapFactory {

  // private static final Logger logger = LogManager.getLogger(LdapUtils.class);

  private static final Map<String, LDAPConnectionPool> openLdapPoolConnection = new HashMap<>();
  private static final Map<String, String> openLdapPoolConnectionConfig = new HashMap<>();
  private static final Map<String, String> openLdapMonoConnectionConfig = new HashMap<>();
  private static final Map<String, LDAPConnection> openLdapMonoConnection = new HashMap<>();

  /**
   * Give an unauthenticated Ldap Connection Pool
   *
   * @param url
   * @return
   * @throws LDAPException
   */
  public static LDAPConnectionPool getConnectionPool(Map<String, String> config)
      throws LDAPException {
    // Check if a ldap connection pool already exist for this userStorage and create
    // it if it doesn't exist
    String key =
        config.get(LdapConfigKeys.REALM_NAME)
            + "_"
            + config.get(LdapConfigKeys.NAME)
            + "_"
            + config.hashCode()
            + "_R";
    String name =
        config.get(LdapConfigKeys.REALM_NAME) + "_" + config.get(LdapConfigKeys.NAME) + "_R";

    if (!openLdapPoolConnectionConfig.containsKey(key)) {
      if (openLdapPoolConnection.containsKey(name)) {
        openLdapPoolConnection.get(name).close();
      }
      openLdapPoolConnectionConfig.put(key, name);
      openLdapPoolConnection.put(
          name,
          new LDAPConnectionPool(
              new LDAPConnection(
                  config.get(LdapConfigKeys.URL), Integer.valueOf(config.get(LdapConfigKeys.PORT))),
              Integer.valueOf(config.get(LdapConfigKeys.POOL_SIZE))));
    }
    return openLdapPoolConnection.get(name);
  }

  public static LDAPConnection getSingleConnection(Map<String, String> config)
      throws LDAPException {
    return getSingleConnection(config, false);
  }

  public static LDAPConnection getSingleConnection(Map<String, String> config, boolean forceErase)
      throws LDAPException {
    String key =
        config.get(LdapConfigKeys.REALM_NAME)
            + "_"
            + config.get(LdapConfigKeys.NAME)
            + "_"
            + config.hashCode()
            + "_R";
    String name =
        config.get(LdapConfigKeys.REALM_NAME) + "_" + config.get(LdapConfigKeys.NAME) + "_R";

    if (!openLdapMonoConnectionConfig.containsKey(key) || forceErase) {
      if (openLdapMonoConnection.containsKey(name) || forceErase) {
        openLdapMonoConnection.get(name).close();
      }
      openLdapMonoConnectionConfig.put(key, name);
      openLdapMonoConnection.put(
          name,
          new LDAPConnection(
              config.get(LdapConfigKeys.URL), Integer.valueOf(config.get(LdapConfigKeys.PORT))));
    }
    return openLdapMonoConnection.get(name);
  }

  /**
   * Give a Ldap Connection Pool
   *
   * @param url
   * @return
   * @throws LDAPException
   */
  public static LDAPConnectionPool getConnectionPoolAuthenticated(Map<String, String> config)
      throws LDAPException {
    // Check if a ldap connection pool already exist for this userStorage and create
    // it if it doesn't exist
    String name =
        config.get(LdapConfigKeys.REALM_NAME) + "_" + config.get(LdapConfigKeys.NAME) + "_RW";
    String key =
        config.get(LdapConfigKeys.REALM_NAME)
            + "_"
            + config.get(LdapConfigKeys.NAME)
            + "_"
            + config.hashCode()
            + "_RW";

    if (!openLdapPoolConnectionConfig.containsKey(key)) {
      if (openLdapPoolConnection.containsKey(name)) {
        openLdapPoolConnection.get(name).close();
      }
      openLdapPoolConnectionConfig.put(key, name);
      openLdapPoolConnection.put(
          name,
          new LDAPConnectionPool(
              new LDAPConnection(
                  config.get(LdapConfigKeys.URL),
                  Integer.valueOf(config.get(LdapConfigKeys.PORT)),
                  config.get(LdapConfigKeys.USERNAME),
                  config.get(LdapConfigKeys.PASSWORD)),
              Integer.valueOf(config.get(LdapConfigKeys.POOL_SIZE))));
    }
    return openLdapPoolConnection.get(name);
  }

  public static LDAPConnection getSingleConnectionAuthenticated(Map<String, String> config)
      throws LDAPException {
    String name =
        config.get(LdapConfigKeys.REALM_NAME) + "_" + config.get(LdapConfigKeys.NAME) + "_RW";
    String key =
        config.get(LdapConfigKeys.REALM_NAME)
            + "_"
            + config.get(LdapConfigKeys.NAME)
            + "_"
            + config.hashCode()
            + "_RW";
    if (!openLdapMonoConnectionConfig.containsKey(key)) {
      if (openLdapMonoConnection.containsKey(name)) {
        openLdapMonoConnection.get(name).close();
      }
      openLdapMonoConnectionConfig.put(key, name);
      openLdapMonoConnection.put(
          name,
          new LDAPConnection(
              config.get(LdapConfigKeys.URL),
              Integer.valueOf(config.get(LdapConfigKeys.PORT)),
              config.get(LdapConfigKeys.USERNAME),
              config.get(LdapConfigKeys.PASSWORD)));
    }
    return openLdapMonoConnection.get(name);
  }

  public static boolean validateUserPassword(
      Map<String, String> config, String userdn, String password) throws LDAPException {
    try (LDAPConnection conn =
        new LDAPConnection(
            config.get(LdapConfigKeys.URL), Integer.valueOf(config.get(LdapConfigKeys.PORT)))) {
      BindResult result = conn.bind(userdn, password);
      if (result.getResultCode().intValue() != 0) {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}
