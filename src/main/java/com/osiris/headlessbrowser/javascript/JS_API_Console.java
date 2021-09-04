package com.osiris.headlessbrowser.javascript;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of: https://developer.mozilla.org/en-US/docs/Web/API/Console_API <br>
 * WORK IN PROGRESS <br>
 *
 * @author Osiris-Team
 */
public class JS_API_Console {
    // This class gets loaded into the JSContext and assigned to a variable with the given name.
    // That means that all public methods/functions and variables/fields in this class are available inside of actual JavaScript code.
    // The method log(String msg) below for example, can be accessed in JavaScript via console.log('Hello!');
    private final PrintStream out;
    private final List<Sendable> onLog = new ArrayList<>();
    private final List<Sendable> onInfo = new ArrayList<>();
    private final List<Sendable> onDebug = new ArrayList<>();
    private final List<Sendable> onError = new ArrayList<>();

    public JS_API_Console(OutputStream out) {
        this(new PrintStream(out));
    }

    public JS_API_Console(PrintStream out) {
        this.out = out;
    }

    public void clear() {
        // Do nothing.
    }

    public void debug(String msg) {
        out.println(msg);
        for (Sendable sendable : onDebug) {
            sendable.send(msg);
        }
    }

    public void error(String msg) {
        out.println(msg);
        for (Sendable sendable : onError) {
            sendable.send(msg);
        }
    }

    public void info(String msg) {
        out.println(msg);
        for (Sendable sendable : onInfo) {
            sendable.send(msg);
        }
    }

    public void log(String msg) {
        out.println(msg);
        for (Sendable sendable : onLog) {
            sendable.send(msg);
        }
    }

    // METHODS FOR JAVA

    public void onLog(Sendable runnable) {
        onLog.add(runnable);
    }

    public void onInfo(Sendable runnable) {
        onInfo.add(runnable);
    }

    public void onDebug(Sendable runnable) {
        onDebug.add(runnable);
    }

    public void onError(Sendable runnable) {
        onError.add(runnable);
    }
        /*
        TODO IMPLEMENT THESE
        TODO GENERATE TEST FOR EACH METHOD.
        TODO Remove from list when implemented.
namespace console { // but see namespace object requirements below
  // Logging
  undefined assert(optional boolean condition = false, any... data);
  undefined table(optional any tabularData, optional sequence<DOMString> properties);
  undefined trace(any... data);
  undefined warn(any... data);
  undefined dir(optional any item, optional object? options);
  undefined dirxml(any... data);

  // Counting
  undefined count(optional DOMString label = "default");
  undefined countReset(optional DOMString label = "default");

  // Grouping
  undefined group(any... data);
  undefined groupCollapsed(any... data);
  undefined groupEnd();

  // Timing
  undefined time(optional DOMString label = "default");
  undefined timeLog(optional DOMString label = "default", any... data);
  undefined timeEnd(optional DOMString label = "default");
};
         */

}
