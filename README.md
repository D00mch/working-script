Below is the text for the article. There is also a code that works.

# Writing a Clojure Script to Open Docker and Two Terminal Windows

If you are looking for an alternative to bash scripts and already know Clojure, consider using it for your scripting needs. While the [Babashka](https://github.com/babashka/babashka) library is a popular choice, there are other options available as well.

Sometimes, we don't want to worry about the command line API. For example, to begin working on my current project, I need to do three things first. Open the Docker application and wait until it's ready. Press `cmd+space` and type "Terminal" to open a terminal emulator. Then, go to the path where the project is located with `cd <path>`. Here, I want to run `docker-compose up`. Next, I press `cmd+t` to open a new tab and run `lein repl` in it.

I know how to do these steps manually, but I don't know how to do it with bash. Using the [robot](https://github.com/D00mch/robot) library, we'll show you how to create a desktop script that *completes these tasks by pressing keys*. Basically, we'll implement it in three steps:

1. Start a docker process (make sure it's running).
2. Open a terminal window in the working directory and run: docker-compose up.
3. Open another terminal tab and run lein repl.

## Preparation / Prerequisites

Before we can begin writing our script, we need to install `leiningen` and `java` in order to create a project and run jars:

```bash
brew install leiningen
brew install java
```

If you have Docker installed, you can use it. If not, you can simulate the work using the `Thread/sleep` function.

Next, we will create a project template using the following command:

```bash
lein new app working
```

Now we will add the `cli` and `robot` dependencies to our `project.clj` file:

```clojure
(defproject working "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [robot/robot "0.2.1-SNAPSHOT"]    ;; new one
                 [org.clojure/tools.cli "1.0.206"] ;; new one
                 ]
  :main ^:skip-aot working.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
```

Next, we will update the `working.core` source file with the following code:

```clojure
(ns working.core
  (:require
   [clojure.tools.cli :as cli])
  (:gen-class))


(def cli-options
  [["-d" "--directory PATH" "Path to directory with the project"
    :default "~/IdeaProjects/work/server/"
    :parse-fn str]])

#_(cli/parse-opts '("-d" "abc") cli-options)

(defn -main [& args]
  (let [{{d :directory} :options} (cli/parse-opts args cli-options)]
    (println "Dir is" d)))
```

Now we can build a `jar` file and run it to ensure everything is working properly:

```bash
lein uberjar                                                            
java -jar target/uberjar/working-0.1.0-SNAPSHOT-standalone.jar -d "path"
```

You should see the output "Dir is path".

## Implementation

To continue working on our script, we will start a REPL by running the following command:

```bash
lein repl
```

If you don't know how to make a REPL connection, consider checking out the [Clojure docs](https://clojure.org/guides/editors) on the subject. The [Show me your REPL](https://www.youtube.com/@smyr-clj/videos) video series is a good place for ideas and inspiration.

After establishing a connection with the REPL, evaluate the namespace `working.core`.

Our first task is to ensure that the docker process is running. To do this, we will add the `clojure.java.shell` dependency and create a `cmd!` function that allows us to run shell commands. We can then use this function to check that we are able to perform actions like `ls`:

```clojure
(ns working.core
  (:require
   [clojure.tools.cli :as cli]
   [clojure.java.shell :refer [sh]])  ;; new one
  (:gen-class))

(defn cmd! [cmd] (sh  "sh" "-c" cmd))

(comment 
  (sh "ls") ;=> 
                  {:exit 0,
                   :out
                   "CHANGELOG.md\nLICENSE\nREADME.md\ndoc\nproject.clj\nresources\nsrc\ntarget\ntest\n",
                   :err ""})
```

With the `cmd!`, we can now create a function that opens the docker process and waits until it is ready:


```clojure
(defn open-docker! [] 
  (cmd! "open -a Docker")
  (while (-> (cmd! "docker stats --no-stream") :out empty?)
    (Thread/sleep 2000)))
```

If you do not have Docker installed, you can use the `Thread/sleep` function to simulate the work. For example, you could replace the `open-docker!` function with the following:

```clojure
(defn open-docker! []
  (Thread/sleep 2000))
```

This will pause the script for 2 seconds, simulating the time it would take to start the docker process. 

Next, we will open a terminal application and run the `docker-compose up` command. I am using `WezTerm` as my terminal client, but you can use any terminal application that you prefer.

```clojure
(defn open-terminal! []
  (cmd! "open /Applications/WezTerm.app"))
```

Or using the `robot` library:

```clojure
(ns working.core
  (:require
   [clojure.tools.cli :as cli]
   [robot.core :as robot]   ;; new one
   [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn open-terminal! []
  (robot/hot-keys! [:cmd :space]) ;; to open spotlight or alfred
  (robot/sleep 100)
  (robot/type-text! "WezTerm")
  (robot/sleep 100)
  (robot/type! :enter))
```

This code will press the cmd and space keys to open the spotlight or Alfred search function, pause for 100 milliseconds, type the text "WezTerm" to search for the terminal application, and press the enter key to launch it.

To open the terminal, we will use the first variant of the `open-terminal!` function. To simplify the process of pasting text, we will add another function.

```clojure
(defn paste! [s]
  (robot/clipboard-put! s)
  (robot/hot-keys! [:cmd :v])
  (robot/sleep 200)
  (robot/type! :enter))
```

With these functions, we can define the `run-docker-compose!` function that opens a terminal, switches to a new tab, and runs the docker-compose up command:

```clojure
(defn run-docker-compose! [path]
  (open-terminal!)
  (robot/sleep 200)
  (robot/hot-keys! [:cmd :t]) ;; be sure to open new tab
  (robot/sleep 200)
  (paste! (str "cd " path))
  (robot/sleep 200)
  (paste! "docker-compose up"))
```

We can also create the `run-repl-in-new-tab!` function to open a new tab and run the REPL command:

```clojure
(defn run-repl-in-new-tab! [path]
  (robot/sleep 100)
  (robot/hot-keys! [:cmd :t])
  (robot/sleep 100)
  (paste! (str "cd " path)) ;; if you terminal opens a new tab in $HOME
  (robot/sleep 100)
  (paste! "lein with-profile +test repl"))
```

To test the `run-repl-in-new-tab!` function, you can use the following code in the REPL:

```clojure
(do 
 (cmd! "open /Applications/WezTerm.app")
 (run-repl-in-new-tab!))
```

Finally, the main function to run all the tasks will look like this:

```clojure
(defn -main [& args]
  (let [{{dir :directory} :options} (cli/parse-opts args cli-options)]
    (println "Starting docker process")
    (open-docker!)
    (println "Running docker-compose in" dir)
    (run-docker-compose! dir)
    (println "Starting lein repl")
    (run-repl-in-new-tab! dir)))
```

This function will parse the command line arguments to get the working directory, start the docker process, run the docker-compose up command in a new terminal tab, and start the `lein repl` in another new tab.

To build and run the script, you can use the following commands:

```bash
lein uberjar

java -jar target/uberjar/working-0.1.0-SNAPSHOT-standalone.jar -d ~/IdeaProjects/work/server/
```

Once we have built the jar file, we can bind it to a hotkey using a tool like [skhd](https://github.com/koekeishiya/skhd). For example, I will bind it to `ctrl+alt+1` by adding the following line to my skhd configuration:

```
ctrl + alt - 1 : java -jar ~/IdeaProjects/clojure/working/target/uberjar/working-0.1.0-SNAPSHOT-standalone.jar -p ~/IdeaProjects/work/server/
```

This allows me to start everything I need for my work with a single keystroke. Here is a demonstration of the script in action (when I press `ctrl+alt+1`):

<p align="center">
  <img src="https://github.com/D00mch/working-script/blob/main/resources/Screen%20Recording%202022-12-27%20at%2001.34.25.gif" width="960" height="600">
</p>

## Possible issues to consider:

Pros:

- This approach allows you to abstract out the low-level command line API and just emit the keys that you would press manually;
- Everything available in the Clojure/Java ecosystem is within your reach, including REPL and all the libraries;
- It works wherever Java works (Linux, OSX, Windows);

Cons:

- If the process that runs the jar file does not have sufficient permissions to manipulate the desktop, robot may not work as expected.
- If we omit the `robot/sleep` calls between commands, we may encounter problems as the keys may be pressed faster than the desktop UI can respond.
- The script may take some time to start. I think it's not critical as the task itself takes several seconds to perform, so the overall time required to complete the task may not be significantly impacted by the startup time of the script.
- The jar file for the described script has a size of 4.8 MB, which you may consider large. This is because it includes all the necessary Java and Clojure core functions.

## How to rewrite this with pure bash

To target both OSX and Linux (with Gnome Terminal support), the bash script would look something like:

```bash
#!/usr/bin/env bash

# Set the terminal emulator to use (default is Terminal on macOS, Gnome Terminal on Linux)
if [[ "$OSTYPE" == "darwin"* ]]; then
  TERMINAL=${2:-Terminal}
else
  TERMINAL=${2:-gnome-terminal}
fi

# Start the docker process
open -a Docker

# Wait until docker is running
while ! docker stats --no-stream; do
  sleep 2
done

# Open $2 terminal emulator, go to $1 path, launch docker-compose
if [[ "$OSTYPE" == "darwin"* ]]; then
  open -a "$TERMINAL"
  osascript -e "tell application \"$TERMINAL\" to activate"
  osascript -e 'tell application "System Events" to keystroke "t" using command down'
  osascript -e "tell application \"System Events\" to keystroke \"cd $1\""
  osascript -e 'tell application "System Events" to keystroke return'
  osascript -e 'tell application "System Events" to keystroke "docker-compose up"'
  osascript -e 'tell application "System Events" to keystroke return'
else
  $TERMINAL --tab --working-directory="$1" -e "docker-compose up"
fi

# Open another tab in the terminal emulator and run lein repl in the specified directory
if [[ "$OSTYPE" == "darwin"* ]]; then
    osascript -e "tell application \"$TERMINAL\" to activate"
    osascript -e 'tell application "System Events" to keystroke "t" using command down'
    osascript -e "tell application \"System Events\" to keystroke \"cd $1\""
    osascript -e 'tell application "System Events" to keystroke return'
    osascript -e 'tell application "System Events" to keystroke "lein with-profile +test repl"'
    osascript -e 'tell application "System Events" to keystroke return'
else
    $TERMINAL --tab -e "bash -c 'cd $1; lein with-profile +test repl'"
fi
```

## Conclusion

Clojure allows us to easily write scripts to manipulate the desktop using the same keys and applications that we would use manually. You have access to a wide range of tools and libraries that make it easy to implement such scripts quickly. If you are already familiar with Clojure, you can avoid having to learn Bash and leverage your existing knowledge. 

However, it is worth noting that there may be issues to consider such as startup time and script size. Regardless, using Clojure with `robot` for desktop scripting can be a convenient solution, that would be reusable on all systems that support Java.

However, it is worth noting that there may be issues to consider, such as startup time and script size. Regardless, using Clojure with robot for desktop scripting is a convenient solution that can be reused on all systems that support Java.

You may find the whole code [here](https://github.com/D00mch/working-script/blob/main/src/working/core.clj).

## Credits

I would like to thank OpenAI's for the assistance in generating some of the content for this article.

## License

Copyright © 2022 Artur Dumchev

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
