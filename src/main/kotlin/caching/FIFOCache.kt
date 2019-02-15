class FIFOCache(capacity: Int) : Cache {
    private val cacheMap = LinkedHashMap<Any, Any>(capacity)

    override fun get(key: Any): Any? = cacheMap.getOrDefault(key, null)

    override fun set(key: Any, value: Any): Any? = cacheMap.put(key, value)
}