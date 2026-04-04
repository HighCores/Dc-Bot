-- ============================================
-- Run this in Supabase SQL Editor BEFORE deploying
-- ============================================

-- Warnings system
CREATE TABLE IF NOT EXISTS dc_warnings (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_name TEXT,
    warned_by TEXT NOT NULL,
    warned_by_name TEXT,
    reason TEXT,
    guild_id TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Giveaways
CREATE TABLE IF NOT EXISTS dc_giveaways (
    id BIGSERIAL PRIMARY KEY,
    message_id TEXT UNIQUE,
    channel_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    host_id TEXT NOT NULL,
    host_name TEXT,
    prize_type TEXT NOT NULL,
    prize_details TEXT,
    currency TEXT,
    coupon_expiry TEXT,
    service_name TEXT,
    discount_info TEXT,
    winner_count INT DEFAULT 1,
    ends_at TIMESTAMPTZ NOT NULL,
    ended BOOLEAN DEFAULT FALSE,
    winners TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Giveaway entries
CREATE TABLE IF NOT EXISTS dc_giveaway_entries (
    id BIGSERIAL PRIMARY KEY,
    giveaway_id BIGINT REFERENCES dc_giveaways(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    entered_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(giveaway_id, user_id)
);

-- Staff Points (for team members on tickets)
CREATE TABLE IF NOT EXISTS dc_points (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    points INT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, guild_id)
);

-- Points history/log
CREATE TABLE IF NOT EXISTS dc_points_log (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    amount INT NOT NULL,
    reason TEXT,
    given_by TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Levels (XP-based, separate from points)
CREATE TABLE IF NOT EXISTS dc_levels (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    xp INT DEFAULT 0,
    level INT DEFAULT 0,
    messages INT DEFAULT 0,
    last_xp_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, guild_id)
);

-- Level rewards (role rewards at specific levels)
CREATE TABLE IF NOT EXISTS dc_level_rewards (
    id BIGSERIAL PRIMARY KEY,
    guild_id TEXT NOT NULL,
    level INT NOT NULL,
    role_id TEXT NOT NULL,
    UNIQUE(guild_id, level)
);

-- Temp roles
CREATE TABLE IF NOT EXISTS dc_temp_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    role_id TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Automod violations
CREATE TABLE IF NOT EXISTS dc_violations (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_name TEXT,
    guild_id TEXT NOT NULL,
    violation_type TEXT,
    details TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
