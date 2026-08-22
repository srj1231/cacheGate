"""Streamlit console for CacheGate MCP server.

This app provides a web interface for interacting with the CacheGate MCP server,
allowing users to send chat completion requests, manage cache, and view provider
information through a persistent connection.
"""
import os
import streamlit as st
from dotenv import load_dotenv
from mcp_client import PersistentMCPClient

load_dotenv()

JAVA_PATH = os.environ.get("CACHEGATE_JAVA_PATH", "java")
JAR_PATH = os.environ.get("CACHEGATE_JAR_PATH")

st.set_page_config(page_title="CacheGate Console", page_icon="🗄️", layout="centered")


@st.cache_resource
def get_client() -> PersistentMCPClient:
    """Create and connect a persistent MCP client.

    This function is cached by Streamlit to maintain a single client instance
    across reruns. The client is initialized with API keys from environment
    variables and connects to the Java-based MCP server.

    Returns:
        A connected PersistentMCPClient instance.

    Raises:
        ValueError: If CACHEGATE_JAR_PATH is not set in environment variables.
        Exception: If connection to the MCP server fails.
    """
    if not JAR_PATH:
        raise ValueError("CACHEGATE_JAR_PATH environment variable is required")

    client = PersistentMCPClient(
        java_path=JAVA_PATH,
        jar_path=JAR_PATH,
        env={
            "GEMINI_API_KEY": os.environ.get("GEMINI_API_KEY", ""),
            "GROQ_API_KEY": os.environ.get("GROQ_API_KEY", ""),
            "OPENROUTER_API_KEY": os.environ.get("OPENROUTER_API_KEY", ""),
        },
    )
    client.connect()
    return client


if "history" not in st.session_state:
    st.session_state.history = []

try:
    client = get_client()
except Exception as e:
    st.error(f"Failed to connect to CacheGate on startup: {e}")
    st.stop()

with st.sidebar:
    st.header("CacheGate Console")
    st.caption("A live, persistent connection to the CacheGate MCP server — no Claude Desktop required.")

    if client.connected:
        st.success("Connected — one CacheGate process, kept alive")
    else:
        st.error(f"Disconnected: {client.last_error or 'unknown reason'}")

    if st.button("🔌 Reconnect", use_container_width=True):
        client.disconnect()
        try:
            client.connect()
            st.success("Reconnected")
        except Exception as e:
            st.error(f"Reconnect failed: {e}")
        st.rerun()

    st.divider()

    if st.button("🔄 Refresh cache stats", use_container_width=True):
        st.info(client.call_tool("cacheStats", {}))

    if st.button("📡 List providers", use_container_width=True):
        st.code(client.call_tool("listProviders", {}))

    st.divider()

    if st.button("🗑️ Clear cache", use_container_width=True):
        st.success(client.call_tool("clearCache", {}))

st.title("🗄️ CacheGate")
st.caption("Live test console — one persistent connection, real cache hits feel instant.")

for entry in st.session_state.history:
    with st.chat_message("user"):
        st.write(entry["prompt"])
    with st.chat_message("assistant"):
        st.write(entry["response"])

col1, col2 = st.columns(2)
with col1:
    use_cache_id = st.number_input("Reuse cache id (optional)", min_value=0, value=0, step=1)
with col2:
    skip_cache = st.checkbox("Skip cache (force fresh call)")

prompt = st.chat_input("Ask CacheGate something...")

if prompt:
    arguments = {"prompt": prompt}
    if use_cache_id > 0:
        arguments["useCacheId"] = int(use_cache_id)
    if skip_cache:
        arguments["skipCache"] = True

    with st.spinner("Calling CacheGate..."):
        try:
            response = client.call_tool("chatCompletion", arguments)
        except Exception as e:
            response = f"Error: {e}"

    st.session_state.history.append({"prompt": prompt, "response": response})
    st.rerun()

