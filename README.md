# Headless-Browser

A headless browser written in Java.

```java
HBrowser hBrowser = new HBrowser();
HWindow hWindow = hBrowser.openNewWindow().load("https://wikipedia.org");
```
## TL;DR

- Can load pages and execute their JavaScript code.
- Has partial JavaScript support. Contributions are needed for implementing all [JS Web-APIs](https://developer.mozilla.org/en-US/docs/Web/API), to achieve full JavaScript support.
- [Click here to see a list of already implemented APIs and how to implement one on your own.](how-to-implement-a-js-web-api.md)

## Contribute

This browser can already load pages and execute their JavaScript code. Problem is that most of the JavaScript code out
there uses so called [Web-APIs](https://developer.mozilla.org/en-US/docs/Web/API), which get shipped with each browser.
Every browser implements them differently, but they are more or less the same accross all browsers.

Since this projects aim is to create a new browser written in Java, completly independent from Chromium, we will need to
implement those APIs by ourselfs. [Click here to see a list of already implemented APIs and how to implement one on your own.](how-to-implement-a-js-web-api.md)
If you are working on an implementation open an issue to keep track of who is working on what and avoid duplicate work.

## Installation

- Java 8 or higher required.
- [Click here for maven/gradle/sbt/leinigen instructions.](https://jitpack.io/#Osiris-Team/Headless-Browser)
- Make sure to watch this repository to get notified of future updates.

## Features

- [x] Free & Open-Source
- [x] Uses Jsoup for editing HTML directly in Java.
- [x] Uses the blazing fast GraalJS-Engine, which supports latest JavaScript code (with latest ECMA specifications).
- [x] Access to all JS-Web APIs from within Java
- [ ] Has all, standard [JavaScript Web-APIs](https://developer.mozilla.org/en-US/docs/Web/API) implemented.

## Motivation

I tried multiple different things like JCEF, Pandomium, Selenium, Selenium based maven dependencies like JWebdriver,
HtmlUnit and maybe some more I don't remember now, but all have one thing in common. They have some kind of very nasty
caveat.

That's why this project exists, to create a completely new browser, not dependent on Chromium or Waterfox or whatever.
We use Jsoup to handle HTML and the GraalJS engine to handle JavaScript. Both are already working and implemented. Only
thing left is implementing the JS Web-APIs.

## Contribute/Build

#### Beginners

If you have never contributed before, we recommend
this [Beginners Article](https://www.jetbrains.com/help/idea/contribute-to-projects.html). If you are planning to make
big changes, create an issue first, where you explain what you want to do. Thank you in advance for every contribution!
If you don't know how to import a GitHub project, check out this
guide: [IntelliJ IDEA Cloning Guide](https://blog.jetbrains.com/idea/2020/10/clone-a-project-from-github/)

#### Build-Details

- Written in [Java](https://java.com/),
  with [JDK 8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html), inside
  of [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- Built with [Maven](https://maven.apache.org/), profiles: clean package

## Links

- https://spec.whatwg.org/ | Details about JS Web-APIs
- https://www.w3.org/TR/?tag=webapi | Details about JS Web-APIs

## Examples

Insert_usage_examples_here

## FAQ

Frequently_asked_questions_here

