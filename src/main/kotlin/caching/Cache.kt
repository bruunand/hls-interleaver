interface Cache {
    operator fun set(key: Any, value: Any): Any?
    operator fun get(key: Any): Any?
}