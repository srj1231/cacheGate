# Troubleshooting

Real issues hit while building CacheGate, kept here in case they save someone else the time they cost me.

**`Empty or null pattern` from Logback on startup.** `logging.pattern.console: ""` doesn't silence console output — Logback rejects an empty pattern outright. Define `logback-spring.xml` with only a file appender; no console appender defined at all means nothing gets written to stdout, no pattern trick needed.

**YAML: `banner-mode: off` (or any boolean-looking value) silently does nothing.** Bare `off`/`on`/`yes`/`no` parse as booleans, not the strings Spring expects. Quote it: `banner-mode: "off"`.

**Custom properties (`groq.*`, `cachegate.*`) fail to resolve — `Could not resolve placeholder`.** Almost always a YAML indentation bug: these blocks need to be top-level, same indentation as `spring:`, not nested underneath it.

**Google GenAI embedding calls fail with "project-id must be set", even though chat works with just an API key.** The embedding module has its own connection config, separate from chat — set `spring.ai.google.genai.embedding.api-key` explicitly too.

**Adding a second chat-model starter breaks the existing Gemini bean — `expected single matching bean but found 2`.** Once a second provider starter is on the classpath, Spring can't guess which `ChatModel`/`EmbeddingModel` a generic request means. Add an explicit `@Qualifier("...")` using the *exact* registered bean name — read it off the relevant `@Bean` factory method (bean name = method name, by convention), or take it from the error message, which lists the real candidates.

**Adding the OpenAI starter crashes on startup with `openAiSdkAudioSpeechModel ... At least one credential source must be specified`, unrelated to anything actually used.** The starter auto-configures OpenAI sub-features (audio, transcription, image) that eagerly build a client from `spring.ai.openai.api-key` at startup regardless of use. Give it any non-empty value — `spring.ai.openai.api-key: ${GROQ_API_KEY}` — since nothing functional routes through it.

**Testing multi-provider fallback by breaking `GEMINI_API_KEY` doesn't test what you'd expect.** The embedding call happens *before* the provider chain and uses the same key — breaking it fails the request early, before the chain is ever reached. To isolate chat fallback specifically, break only `chat.options.model` (a typo'd name), leaving both API keys and the embedding model untouched.

**Inconsistent cache behavior — hits reported that don't show up in the database.** Means more than one instance of the jar is running at once. Check with `ps aux | grep cachegate` and kill anything unexpected before restarting. The same thing can happen between Claude Desktop and the Streamlit console if both are open simultaneously — two separate processes, each with its own in-memory cache that only syncs from disk at its own startup.

**`pip install mcp` fails with "Could not find a version that satisfies the requirement".** The `mcp` package requires Python 3.10+. `python3 -m venv venv` uses whatever `python3` your system defaults to — on macOS that's often the old Xcode Command Line Tools Python (3.9). Check with `which python3` after activating; if it's not 3.10+, install a real one (`brew install python@3.12`) and rebuild the venv against that specific binary.

**`streamlit: command not found` even though the install "succeeded".** Two different virtual environments likely exist — check your shell prompt for the *exact* active one (`(venv)` vs `(.venv)` are not the same). Some editors auto-create a default `.venv` in the project root the moment they need one.

**Editor shows "No module named 'streamlit'" even though the terminal install worked.** The editor's interpreter setting is separate from your terminal's active shell. Point it explicitly at `streamlit_console/venv/bin/python3.12`.