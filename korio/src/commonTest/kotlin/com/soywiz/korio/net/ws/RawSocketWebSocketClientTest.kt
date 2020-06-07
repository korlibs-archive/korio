package com.soywiz.korio.net.ws

import com.soywiz.korio.async.*
import com.soywiz.korio.net.http.Http
import kotlin.test.*

class RawRawSocketWebSocketClient {
    @Test
    fun test() = suspendTestNoJs {
        assertEquals(
            "GET / HTTP/1.1",
            (RawSocketWebSocketClient(
                "ws://127.0.0.1:8081/",
                connect = false
            ) as RawSocketWebSocketClient).buildHeader().split("\r\n").first()
        )
    }

    @Test
    fun test2() = suspendTestNoJs {
        val ws =
            RawSocketWebSocketClient("ws://127.0.0.1:8081/path?id=someId", connect = false) as RawSocketWebSocketClient
        assertEquals(
            "GET /path?id=someId HTTP/1.1\r\n" +
                "Host: 127.0.0.1:8081\r\n" +
                "Pragma: no-cache\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Upgrade: websocket\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: d3NrZXk=\r\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36\r\n" +
                "\r\n",
            ws.buildHeader()
        )
    }

    @Test
    fun test3() = suspendTestNoJs {
        val ws =
            RawSocketWebSocketClient("ws://127.0.0.1:8081/path?id=someId", connect = false, headers = Http.Headers.build {
                put("user-agent", "myagent!")
            }) as RawSocketWebSocketClient
        assertEquals(
            "GET /path?id=someId HTTP/1.1\r\n" +
                "Host: 127.0.0.1:8081\r\n" +
                "Pragma: no-cache\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Upgrade: websocket\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: d3NrZXk=\r\n" +
                "user-agent: myagent!\r\n" +
                "\r\n",
            ws.buildHeader()
        )
    }
}
