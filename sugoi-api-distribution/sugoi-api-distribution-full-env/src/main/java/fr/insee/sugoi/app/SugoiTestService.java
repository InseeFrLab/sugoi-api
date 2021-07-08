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
package fr.insee.sugoi.app;

import fr.insee.sugoi.app.service.BrokerEmbeddedService;
import fr.insee.sugoi.app.service.LdapEmbeddedService;
import fr.insee.sugoi.app.service.TomcatEmbeddedService;
import fr.insee.sugoi.app.service.utils.PropertiesLoaderService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SugoiTestService {

  private static boolean fork;

  private static boolean enabledEmbeddedLdap =
      Boolean.parseBoolean(
          PropertiesLoaderService.load("fr.insee.sugoi.full.env.ldap.embedded.enabled", "true"));

  private static boolean enabledEmbeddedBroker =
      Boolean.parseBoolean(
          PropertiesLoaderService.load("fr.insee.sugoi.full.env.broker.embedded.enabled", "true"));

  private static boolean enabledEmbeddedTomcat =
      Boolean.parseBoolean(
          PropertiesLoaderService.load("fr.insee.sugoi.full.env.tomcat.embedded.enabled", "true"));
  private static int httpPortEmbeddedTomcat =
      Integer.parseInt(
          PropertiesLoaderService.load("fr.insee.sugoi.full.env.tomcat.port.http", "8080"));
  private static int httpsPortEmbeddedTomcat =
      Integer.parseInt(
          PropertiesLoaderService.load("fr.insee.sugoi.full.env.tomcat.port.https", "8443"));

  private static String nameEmbeddedTomcat1 =
      PropertiesLoaderService.load("fr.insee.sugoi.full.env.tomcat1.name", "tomcat1");
  private static String propertiesFileEmbeddedTomcat1 =
      PropertiesLoaderService.load(
          "fr.insee.sugoi.full.env.tomcat1.properties.file",
          "/tomcat-properties/tomcat1.properties");

  private static String nameEmbeddedTomcat2 =
      PropertiesLoaderService.load("fr.insee.sugoi.full.env.tomcat2.name", "tomcat2");
  private static String propertiesFileEmbeddedTomcat2 =
      PropertiesLoaderService.load(
          "fr.insee.sugoi.full.env.tomcat2.properties.file",
          "/tomcat-properties/tomcat2.properties");

  /**
   * Démarrage des services de test.
   *
   * @throws JAXBException
   */
  public static void main(String[] args) throws InterruptedException, IOException {

    if (args.length == 0 || "start".equalsIgnoreCase(args[0])) {
      if (args.length > 1 && args[1].equals("fork")) {
        fork = true;
      }
      startServers();
    }
    if (args.length > 0 && "stop".equalsIgnoreCase(args[0])) {
      Socket socket = new Socket("localhost", 4567);
      socket.getInputStream();
      socket.close();
    }
  }

  private static void startServers() throws InterruptedException {
    ExecutorService execs =
        Executors.newFixedThreadPool(
            4,
            new ThreadFactory() {

              @Override
              public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
              }
            });
    if (enabledEmbeddedLdap) {
      execs.submit(
          () -> {
            try {
              LdapEmbeddedService.start();
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }
    if (enabledEmbeddedBroker) {
      execs.submit(
          () -> {
            try {
              BrokerEmbeddedService.start();
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }
    if (enabledEmbeddedTomcat) {
      execs.submit(
          () -> {
            try {
              TomcatEmbeddedService.start(
                  nameEmbeddedTomcat1,
                  propertiesFileEmbeddedTomcat1,
                  nameEmbeddedTomcat2,
                  propertiesFileEmbeddedTomcat2,
                  httpPortEmbeddedTomcat,
                  httpsPortEmbeddedTomcat);
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }
    execs.submit(
        () -> {
          try {
            ServerSocket shutdownSocket = new ServerSocket(4567);
            Socket sock = shutdownSocket.accept();
            sock.close();
            shutdownSocket.close();
            stopAll();
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

    if (!fork) {
      execs.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } else {
      System.out.println("sleeping ...");
      Thread.sleep(60000);
      System.out.println("continue");
    }
  }

  private static void stopAll() throws Exception {

    TomcatEmbeddedService.stopAll();

    if (enabledEmbeddedLdap) {
      LdapEmbeddedService.stop();
    }

    if (enabledEmbeddedBroker) {
      BrokerEmbeddedService.stop();
    }
  }
}
