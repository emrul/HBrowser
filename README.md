# HBrowser

Another headless browser for Java with Puppeteer and Playwright implemented.
Add this to your project with [Maven/Gradle/Sbt/Leinigen](https://jitpack.io/#Osiris-Team/HBrowser/LATEST)
(Java 8 or higher required).

```java
HBrowser hBrowser = new HBrowser();
try(PlaywrightWindow window = hBrowser.openWindow()){
    window.load("https://example.com");
   // ...   
}
```
All examples [here](src/test/java/examples).
Note that the first run may take a bit because Node.js and its modules get installed into your current working dir under `./headless-browser`.

### Features
 - High-Level methods for downloading files, working with cookies, retrieving HTML and simulating real user input. 
 - Easy access to Node.js from within Java.
 - HTML handling via Jsoup and JSON with Gson.

### Drivers
Playwright is the default and recommended browser driver to use, since it supports downloads
and more of its features were ported to Java.
Checkout [JG-Browser](https://github.com/Osiris-Team/JG-Browser) for a browser completely written in Java.

| Name | Version| JS-Engine | Downloads |
| :-----: | :-----: | :-----: | :-----:
| [Playwright](https://github.com/microsoft/playwright)| Latest | Node.js/V8 | Yes | No |
| [Puppeteer](https://github.com/puppeteer/puppeteer) | Latest  | Node.js/V8 | No | No |

(JS = JavaScript; Downloads = If the browser is able to download files other than html/xml/pdf;)

### [Contribute/Build](CONTRIBUTE.md)

### Libraries

| Name/Link | Usage | License |
| :-----: | :-----: | :-----: |
| [Playwright](https://github.com/microsoft/playwright) | Emulates different types of browsers | [License](https://github.com/microsoft/playwright/blob/master/LICENSE) |
| [Puppeteer](https://github.com/puppeteer/puppeteer) | Emulates different types of browsers  | [License](https://github.com/puppeteer/puppeteer/blob/main/LICENSE) |
| [Node.js](https://github.com/nodejs/node) | Enables executing JavaScript code | [License](https://github.com/nodejs/node/blob/master/LICENSE) |
| [Jsoup](https://github.com/jhy/jsoup)      | Used to load pages and modify their HTML code      |   [License](https://github.com/jhy/jsoup/blob/master/LICENSE) |
