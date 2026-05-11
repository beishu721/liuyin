package com.liuyin.app.network.parser

data class ParserConfig(
    val version: Int = 1,
    val api: ApiConfig = ApiConfig()
)

data class ApiConfig(
    val view: EndpointConfig = EndpointConfig(),
    val playurl: EndpointConfig = EndpointConfig()
)

data class EndpointConfig(
    val url: String = "",
    val headers: Map<String, String> = emptyMap()
)