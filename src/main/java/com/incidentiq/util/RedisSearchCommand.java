package com.incidentiq.util;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Minimal enum to allow sending FT.SEARCH (RediSearch) via Jedis sendCommand.
 */
public enum RedisSearchCommand implements ProtocolCommand {
    FT_SEARCH("FT.SEARCH"),
    FT_CREATE("FT.CREATE"),
    FT_DROPINDEX("FT.DROPINDEX");

    private final byte[] raw;

    RedisSearchCommand(String alt) {
        raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }
}
