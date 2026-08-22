from __future__ import annotations

import asyncio
import threading
from contextlib import AsyncExitStack
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

class PersistentMCPClient:
    """A persistent MCP client that runs an asyncio event loop in a background thread.

    This client maintains a long-lived connection to the MCP server (Java-based)
    and allows synchronous method calls from non-async code by bridging to the
    background event loop.

    Args:
        java_path: Path to the Java executable.
        jar_path: Path to the MCP server JAR file.
        env: Environment variables to pass to the Java process.
    """
    def __init__(self, java_path: str, jar_path: str, env: dict):
        self.java_path = java_path
        self.jar_path = jar_path
        self.env = env

        self.loop = asyncio.new_event_loop()
        self.thread = threading.Thread(target=self._run_loop, daemon=True)
        self.thread.start()

        self.exit_stack: AsyncExitStack | None = None
        self.session: ClientSession | None = None
        self.connected = False
        self.last_error: str | None = None

    def _run_loop(self):
        """Run the asyncio event loop forever in the background thread."""
        asyncio.set_event_loop(self.loop)
        self.loop.run_forever()

    def _submit(self, coro: asyncio.Coroutine, timeout: float):
        """Submit a coroutine to the background event loop and wait for its result.

        Args:
            coro: The coroutine to execute.
            timeout: Maximum time to wait for the coroutine to complete.

        Returns:
            The result of the coroutine.

        Raises:
            concurrent.futures.TimeoutError: If the coroutine doesn't complete within timeout.
        """
        future = asyncio.run_coroutine_threadsafe(coro, self.loop)
        return future.result(timeout=timeout)

    async def _connect(self):
        """Establish a connection to the MCP server.

        Creates stdio server parameters, initializes the client session,
        and marks the client as connected.
        """
        self.exit_stack = AsyncExitStack()
        server_params = StdioServerParameters(
            command=self.java_path,
            args=["-jar", self.jar_path],
            env=self.env,
        )
        read, write = await self.exit_stack.enter_async_context(stdio_client(server_params))
        self.session = await self.exit_stack.enter_async_context(ClientSession(read, write))
        await self.session.initialize()
        self.connected = True

    async def _disconnect(self):
        """Close the connection to the MCP server and clean up resources."""
        if self.exit_stack is not None:
            await self.exit_stack.aclose()
        self.session = None
        self.exit_stack = None
        self.connected = False
        self.last_error = None

    async def _call_tool(self, name: str, arguments: dict) -> str:
        """Call a tool on the MCP server.

        Args:
            name: The name of the tool to call.
            arguments: The arguments to pass to the tool.

        Returns:
            The text content of the tool result.

        Raises:
            IndexError: If the result has no content items.
        """
        result = await self.session.call_tool(name, arguments)
        if not result.content:
            raise ValueError(f"Tool '{name}' returned no content")
        return result.content[0].text

    def connect(self, timeout: float = 30):
        """Connect to the MCP server synchronously.

        Args:
            timeout: Maximum time to wait for connection (default: 30 seconds).

        Raises:
            Exception: If connection fails. The error is stored in last_error.
        """
        try:
            self._submit(self._connect(), timeout=timeout)
            self.last_error = None
        except Exception as e:
            self.connected = False
            self.last_error = str(e)
            raise

    def disconnect(self):
        """Disconnect from the MCP server synchronously.

        Silently ignores errors during disconnection to ensure cleanup
        always completes.
        """
        try:
            self._submit(self._disconnect(), timeout=10)
        except Exception:
            pass

    def call_tool(self, name: str, arguments: dict, timeout: float = 90) -> str:
        """Call a tool on the MCP server synchronously.

        Args:
            name: The name of the tool to call.
            arguments: The arguments to pass to the tool.
            timeout: Maximum time to wait for the tool to complete (default: 90 seconds).

        Returns:
            The text content of the tool result.

        Raises:
            RuntimeError: If not connected to the server.
        """
        if not self.connected:
            raise RuntimeError("Not connected — use the Reconnect button in the sidebar.")
        return self._submit(self._call_tool(name, arguments), timeout=timeout)

