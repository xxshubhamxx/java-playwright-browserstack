package runners;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.*;

import com.microsoft.playwright.*;
import com.google.gson.JsonObject;
import java.net.URLEncoder;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import utils.SetupLocalTesting;

import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

public class BstackRunner implements TestTemplateInvocationContextProvider {
    public Browser browser;
    public String username, accessKey, wss;
    private JSONObject mainConfig;
    private JSONArray platformConfig;
    private Map<String, Object> commonCapsConfig;


    public BstackRunner() {
        this.username = setupCredsAndServer().get("username");
        this.accessKey = setupCredsAndServer().get("accesskey");
        this.wss = setupCredsAndServer().get("wss");
    }

    public HashMap<String, String> setupCredsAndServer() {
        try {
            if (System.getProperty("config") != null) {
                JSONParser parser = new JSONParser();
                mainConfig = (JSONObject) parser
                        .parse(new FileReader("src/test/resources/conf/" + System.getProperty("config")));
            }
            wss = (String) mainConfig.get("wss");
            username = System.getenv("BROWSERSTACK_USERNAME");
            if (username == null) {
                username = (String) mainConfig.get("userName");
            }
            accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
            if (accessKey == null) {
                accessKey = (String) mainConfig.get("accessKey");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        HashMap<String, String> creds = new HashMap();
        creds.put("username", username);
        creds.put("accesskey", accessKey);
        creds.put("wss", wss);
        return creds;
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        List<TestTemplateInvocationContext> desiredCapsInvocationContexts = new ArrayList<>();

        try {
            platformConfig = (JSONArray) mainConfig.get("environments");
            commonCapsConfig = (Map<String, Object>) mainConfig.get("capabilities");

            for (int i = 0; i < platformConfig.size(); i++) {
                JSONObject platform = (JSONObject) platformConfig.get(i);
                platform.putAll(commonCapsConfig);
                platform.put("browserstack.username", username);
                platform.put("browserstack.accessKey", accessKey);
                if(System.getProperty("local") == "true") {
                    platform.put("browserstack.local", "true");
                    HashMap<String, String> localOptions = new HashMap<>();
                    localOptions.put("key", accessKey);
                    //Add more local options here, e.g. forceLocal, localIdentifier, etc.
                    SetupLocalTesting.createInstance(localOptions);
                }
                desiredCapsInvocationContexts.add(invocationContext(platform));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return desiredCapsInvocationContexts.stream();
    }

    private TestTemplateInvocationContext invocationContext(JSONObject capabilitiesObject) {
        return new TestTemplateInvocationContext() {

            @Override
            public List<Extension> getAdditionalExtensions() {

                return Collections.singletonList(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext,
                                                     ExtensionContext extensionContext) {
                        return parameterContext.getParameter().getType().equals(Browser.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext,
                                                   ExtensionContext extensionContext) {
                        try(Playwright playwright = Playwright.create()) {
                            BrowserType browserType = playwright.chromium();
                            String caps = URLEncoder.encode(capabilitiesObject.toString(), "utf-8");
                            System.out.println(capabilitiesObject.toString());
                            String ws_endpoint = wss + "?caps=" + caps;
                            browser = browserType.connect(ws_endpoint);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return browser;
                    }
                });
            }
        };
    }
}
