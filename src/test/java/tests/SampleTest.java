package tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import runners.PlaywrightTest;

public class SampleTest {

    @PlaywrightTest
    void singleTest(Browser browser) {
        Page page = browser.newPage();
        try {
            page.navigate("https://bstackdemo.com/");
            String product_name = page.locator("//*[@id='1']/p").textContent();
            page.locator("//*[@id='1']/div[4]").click();
            page.locator(".float\\-cart__content");
            String product_in_cart = page.locator("//*[@id='__next']/div/div/div[2]/div[2]/div[2]/div/div[3]/p[1]").textContent();
            if (product_name.equals(product_in_cart)) {
                page.evaluate("_ => {}", "browserstack_executor: { \"action\": \"setSessionStatus\", \"arguments\": { \"status\": \"" + "passed" + "\", \"reason\": \"" + "Product has been successfully added to the cart!" + "\"}}");
            } else {
                page.evaluate("_ => {}", "browserstack_executor: { \"action\": \"setSessionStatus\", \"arguments\": { \"status\": \"" + "failed" + "\", \"reason\": \"" + "There was some issue!" + "\"}}");
            }
        } catch (Exception e) {
            page.evaluate("_ => {}", "browserstack_executor: { \"action\": \"setSessionStatus\", \"arguments\": { \"status\": \"" + "failed" + "\", \"reason\": \"" + "There was some issue!" + "\"}}");
            System.out.println("Exception: " + e.getMessage());
        }
        browser.close();
    }
}
