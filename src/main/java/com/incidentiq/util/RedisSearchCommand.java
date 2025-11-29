package com.incidentiq.util;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public enum RedisSearchCommand implements ProtocolCommand {

    FT_CREATE("FT.CREATE"),
    FT_SEARCH("FT.SEARCH"),
    FT_INFO("FT.INFO"),
    FT_DROPINDEX("FT.DROPINDEX");

    private final byte[] raw;

    RedisSearchCommand(String cmd) {
        this.raw = SafeEncoder.encode(cmd);
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }
}
